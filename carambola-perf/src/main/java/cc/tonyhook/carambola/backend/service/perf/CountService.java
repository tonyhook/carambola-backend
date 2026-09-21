package cc.tonyhook.carambola.backend.service.perf;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventHourlyRepository;
import cc.tonyhook.carambola.backend.dao.perf.EventRepository;
import cc.tonyhook.carambola.backend.entity.perf.Client;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.entity.perf.CountView;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;

@Service
public class CountService {

    private final EventRepository eventRepository;
    private final EventHourlyRepository eventHourlyRepository;
    private final EventHourlyService eventHourlyService;
    private final ClientChannelService clientChannelService;
    private final EventCatalogService eventCatalogService;

    public CountService(
            EventRepository eventRepository,
            EventHourlyRepository eventHourlyRepository,
            EventHourlyService eventHourlyService,
            ClientChannelService clientChannelService,
            EventCatalogService eventCatalogService
    ) {
        this.eventRepository = eventRepository;
        this.eventHourlyRepository = eventHourlyRepository;
        this.eventHourlyService = eventHourlyService;
        this.clientChannelService = clientChannelService;
        this.eventCatalogService = eventCatalogService;
    }

    public List<CountView> queryCountList(
            Authentication authentication,
            Timestamp start,
            Timestamp end,
            String interval,
            Integer timezoneOffset,
            String level,
            Integer clientId,
            Integer clientProjectId,
            String media,
            String operator
    ) {
        List<ClientChannel> clientChannels = clientChannelService.getClientChannelList(authentication);
        Map<Integer, ClientChannel> accessibleClientChannels = new HashMap<Integer, ClientChannel>();
        for (ClientChannel clientChannel : clientChannels) {
            accessibleClientChannels.put(clientChannel.getId(), clientChannel);
        }
        if (accessibleClientChannels.isEmpty()) {
            return List.of();
        }

        // 同一 (时间, 渠道, 事件) 可能来自多段查询,addRow 负责累加
        Map<String, CountView> countMap = new HashMap<String, CountView>();
        for (Object[] row : aggregate(start, end, interval, timezoneOffset, accessibleClientChannels.keySet())) {
            RowData data = new RowData(row, accessibleClientChannels);
            addRow(countMap, data, level, clientId, clientProjectId, media, operator, accessibleClientChannels.keySet());
        }

        List<CountView> counts = new ArrayList<CountView>(countMap.values());
        counts.sort(Comparator
            .comparing(CountView::getTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(CountView::getClientName, Comparator.nullsLast(String::compareTo))
            .thenComparing(CountView::getClientProjectName, Comparator.nullsLast(String::compareTo))
            .thenComparing(CountView::getMedia, Comparator.nullsLast(String::compareTo))
            .thenComparing(CountView::getOperator, Comparator.nullsLast(String::compareTo)));

        return counts;
    }

    // 整点区段读小时汇总,两头不满一小时的零头和水位之后尚未汇总的部分读明细;去重人数不能相加,整个窗口单独从明细算
    private List<Object[]> aggregate(
            Timestamp start,
            Timestamp end,
            String interval,
            Integer timezoneOffset,
            Set<Integer> clientChannelIds
    ) {
        List<Object[]> rows = new ArrayList<Object[]>();
        RolledRange rolled = rolledRange(start, end, timezoneOffset, eventHourlyService.getRolledUntil());
        if (rolled == null) {
            rows.addAll(eventRepository.aggregateTotals(start, end, interval, timezoneOffset, clientChannelIds));
        } else {
            if (start.before(rolled.start())) {
                rows.addAll(eventRepository.aggregateTotals(start, rolled.start(), interval, timezoneOffset, clientChannelIds));
            }
            rows.addAll(eventHourlyRepository.aggregate(rolled.start(), rolled.end(), interval, timezoneOffset, clientChannelIds));
            if (rolled.end().before(end)) {
                rows.addAll(eventRepository.aggregateTotals(rolled.end(), end, interval, timezoneOffset, clientChannelIds));
            }
        }
        rows.addAll(eventRepository.aggregateUserCount(
            start, end, interval, timezoneOffset, clientChannelIds, EventCodes.userCounted()));

        return rows;
    }

    record RolledRange(Timestamp start, Timestamp end) {
    }

    // [start, end) 中能交给小时汇总的部分:完整落在水位之前的整点,没有则为 null。
    // 时区偏移不是整小时时,UTC 整点拼不出本地的时和日,只能全走明细
    static RolledRange rolledRange(Timestamp start, Timestamp end, Integer timezoneOffset, Timestamp rolledUntil) {
        if (rolledUntil == null || timezoneOffset % 60 != 0) {
            return null;
        }
        Timestamp rolledStart = EventHourlyService.ceilHour(start);
        Timestamp rolledEnd = EventHourlyService.floorHour(end);
        if (rolledUntil.before(rolledEnd)) {
            rolledEnd = rolledUntil;
        }

        return rolledStart.before(rolledEnd) ? new RolledRange(rolledStart, rolledEnd) : null;
    }

    private void addRow(
            Map<String, CountView> countMap,
            RowData data,
            String level,
            Integer clientId,
            Integer clientProjectId,
            String media,
            String operator,
            Set<Integer> accessibleClientChannelIds
    ) {
        if (data.clientChannelId != null && !accessibleClientChannelIds.contains(data.clientChannelId)) {
            return;
        }
        if (data.event == null) {
            return;
        }
        if (clientId != null && !clientId.equals(data.clientId)) {
            return;
        }
        if (clientProjectId != null && !clientProjectId.equals(data.clientProjectId)) {
            return;
        }
        if (media != null && !media.equals(data.media)) {
            return;
        }
        if (operator != null && !operator.equals(data.operatorFilterValue())) {
            return;
        }
        CountView count = countMap.computeIfAbsent(data.countKey(level), key -> data.toCountView(level));
        count.getEventCounts().put(data.event, count.getEventCounts().getOrDefault(data.event, 0L) + data.count);
        count.getEventUserCounts().put(data.event, count.getEventUserCounts().getOrDefault(data.event, 0L) + data.userCount);
        count.getEventRawCounts().put(data.event, count.getEventRawCounts().getOrDefault(data.event, 0L) + data.rawCount);
        if (data.amount != null) {
            count.getEventAmounts().put(data.event, count.getEventAmounts().getOrDefault(data.event, BigDecimal.ZERO).add(data.amount));
        }
        if (data.cost != null) {
            count.setSpend(count.getSpend().add(data.cost));
        }
        count.getEventNames().putIfAbsent(data.event, eventCatalogService.get(data.event).name());
    }

    private static String key(Object... values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            builder.append(value == null ? "" : value.toString()).append("|");
        }
        return builder.toString();
    }

    private static class RowData {

        private final String time;
        private final Integer clientChannelId;
        private final Integer clientId;
        private final String clientName;
        private final Integer clientProjectId;
        private final String clientProjectName;
        private final String media;
        private final String mediaCode;
        private final String operator;
        private final String event;
        private final Long count;
        private final Long rawCount;
        private final BigDecimal amount;
        private final BigDecimal cost;
        private final Long userCount;

        // row 的列顺序见 EventRepository.aggregateTotals
        RowData(Object[] row, Map<Integer, ClientChannel> clientChannels) {
            this.time = string(row[0]);
            this.clientChannelId = integer(row[1]);
            this.event = string(row[2]);
            this.count = number(row[3]);
            this.rawCount = number(row[4]);
            this.amount = decimal(row[5]);
            this.cost = decimal(row[6]);
            this.userCount = number(row[7]);

            ClientChannel clientChannel = clientChannels.get(this.clientChannelId);
            Client client = clientChannel == null ? null : clientChannel.getClient();
            ClientProject clientProject = clientChannel == null ? null : clientChannel.getClientProject();
            this.clientId = client == null ? null : client.getId();
            this.clientName = client == null ? null : client.getName();
            this.clientProjectId = clientProject == null ? null : clientProject.getId();
            this.clientProjectName = clientProject == null ? null : clientProject.getName();
            this.media = clientChannel == null ? null : clientChannel.getMediaName();
            this.mediaCode = clientChannel == null ? null : clientChannel.getMediaCode();
            this.operator = clientChannel == null ? null : clientChannel.getOperator();
        }

        String countKey(String level) {
            return key(
                this.time,
                include(level, "client") ? this.clientId : null,
                include(level, "project") ? this.clientProjectId : null,
                include(level, "channel") ? this.clientChannelId : null);
        }

        CountView toCountView(String level) {
            CountView count = new CountView();
            count.setTime(this.time);
            if (include(level, "client")) {
                count.setClientId(this.clientId);
                count.setClientName(this.clientName);
            }
            if (include(level, "project")) {
                count.setClientProjectId(this.clientProjectId);
                count.setClientProjectName(this.clientProjectName);
            }
            if (include(level, "channel")) {
                count.setClientChannelId(this.clientChannelId);
                count.setMedia(this.media);
                count.setMediaCode(this.mediaCode);
                count.setOperator(this.operator);
            }

            return count;
        }

        String operatorFilterValue() {
            if (this.operator != null && !this.operator.isBlank()) {
                return this.operator;
            }

            return this.media;
        }

        private static boolean include(String level, String dimension) {
            int levelIndex = order(level);
            int dimensionIndex = order(dimension);

            return levelIndex >= dimensionIndex;
        }

        private static int order(String level) {
            if ("channel".equals(level)) {
                return 3;
            }
            if ("project".equals(level)) {
                return 2;
            }

            return 1;
        }

        private static String string(Object value) {
            return value == null ? null : value.toString();
        }

        private static Integer integer(Object value) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                return Integer.valueOf(value.toString());
            }

            return null;
        }

        private static Long number(Object value) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                return Long.valueOf(value.toString());
            }

            return 0L;
        }

        private static BigDecimal decimal(Object value) {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            if (value != null) {
                return new BigDecimal(value.toString());
            }

            return null;
        }
    }

}
