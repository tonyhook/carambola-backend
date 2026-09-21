package cc.tonyhook.carambola.backend.service.perf;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cc.tonyhook.carambola.backend.dao.perf.ClientChannelRepository;
import cc.tonyhook.carambola.backend.dao.perf.EventHourlyProgressRepository;
import cc.tonyhook.carambola.backend.dao.perf.EventHourlyRepository;
import cc.tonyhook.carambola.backend.dao.perf.EventRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.EventHourly;
import cc.tonyhook.carambola.backend.entity.perf.EventHourlyProgress;

// 把已经结束的整点逐个汇总进 perf_event_hourly。
// 事件写入后参与统计的字段不再变化,一个小时汇总一次就是定论;重跑同一小时是先删后写,结果不变。
@Service
public class EventHourlyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHourlyService.class);

    static final long HOUR_MILLIS = Duration.ofHours(1).toMillis();

    // 小时结束后再等这么久才汇总,留给在途的写入落库
    private static final Duration SETTLE_DELAY = Duration.ofMinutes(5);

    // 每轮最多汇总的小时数;首次上线时靠它把历史回填拆成小批
    private static final int HOURS_PER_RUN = 24;

    private final EventRepository eventRepository;
    private final EventHourlyRepository eventHourlyRepository;
    private final EventHourlyProgressRepository eventHourlyProgressRepository;
    private final ClientChannelRepository clientChannelRepository;
    private final TransactionTemplate transactionTemplate;

    public EventHourlyService(
            EventRepository eventRepository,
            EventHourlyRepository eventHourlyRepository,
            EventHourlyProgressRepository eventHourlyProgressRepository,
            ClientChannelRepository clientChannelRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.eventRepository = eventRepository;
        this.eventHourlyRepository = eventHourlyRepository;
        this.eventHourlyProgressRepository = eventHourlyProgressRepository;
        this.clientChannelRepository = clientChannelRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // 汇总表覆盖到的时刻(不含),尚未开始汇总时为 null
    public Timestamp getRolledUntil() {
        return this.eventHourlyProgressRepository.findById(EventHourlyProgress.SINGLETON_ID)
            .map(EventHourlyProgress::getRolledUntil)
            .orElse(null);
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void rollup() {
        List<Integer> clientChannelIds = new ArrayList<Integer>();
        for (ClientChannel clientChannel : this.clientChannelRepository.findAll()) {
            clientChannelIds.add(clientChannel.getId());
        }
        if (clientChannelIds.isEmpty()) {
            return;
        }

        Timestamp next = this.getRolledUntil();
        if (next == null) {
            Timestamp firstEventTime = this.eventRepository.findFirstEventTime();
            if (firstEventTime == null) {
                return;
            }
            next = floorHour(firstEventTime);
        }

        Timestamp settled = floorHour(Timestamp.from(Instant.now().minus(SETTLE_DELAY)));
        for (int i = 0; i < HOURS_PER_RUN && next.before(settled); i++) {
            Timestamp hourStart = next;
            try {
                this.transactionTemplate.executeWithoutResult(status -> rollupHour(hourStart, clientChannelIds));
            } catch (RuntimeException e) {
                LOGGER.warn("Event hourly rollup failed: hour={}", hourStart, e);
                return;
            }
            next = new Timestamp(hourStart.getTime() + HOUR_MILLIS);
        }
    }

    private void rollupHour(Timestamp hourStart, List<Integer> clientChannelIds) {
        Timestamp hourEnd = new Timestamp(hourStart.getTime() + HOUR_MILLIS);

        // 普通 SELECT 是快照读,不给 perf_event 加锁;INSERT ... SELECT 会加 next-key 锁挡住新事件写入
        List<EventHourly> hourlies = new ArrayList<EventHourly>();
        for (Object[] row : this.eventRepository.aggregateTotals(hourStart, hourEnd, "hour", 0, clientChannelIds)) {
            EventHourly hourly = new EventHourly();
            hourly.setHourStart(hourStart);
            hourly.setClientChannelId(((Number) row[1]).intValue());
            hourly.setEvent((String) row[2]);
            hourly.setEventCount(((Number) row[3]).longValue());
            hourly.setRawCount(((Number) row[4]).longValue());
            hourly.setAmount(decimal(row[5]));
            hourly.setCost(decimal(row[6]));
            hourlies.add(hourly);
        }

        this.eventHourlyRepository.deleteByHourStart(hourStart);
        this.eventHourlyRepository.saveAll(hourlies);

        EventHourlyProgress progress = new EventHourlyProgress();
        progress.setId(EventHourlyProgress.SINGLETON_ID);
        progress.setRolledUntil(hourEnd);
        this.eventHourlyProgressRepository.save(progress);
    }

    static Timestamp floorHour(Timestamp time) {
        return new Timestamp(Math.floorDiv(time.getTime(), HOUR_MILLIS) * HOUR_MILLIS);
    }

    static Timestamp ceilHour(Timestamp time) {
        return new Timestamp(Math.ceilDiv(time.getTime(), HOUR_MILLIS) * HOUR_MILLIS);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        return new BigDecimal(value.toString());
    }

}
