package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// perf_event_callback_url 按 event_id 分段滚动:前面始终预留空分区,整段都过了保留期就 DROP 掉,
// 空间立即归还,不像 DELETE 那样留下碎片和大量 undo。
// 按 id 而不按时间分段,主键不必带上 time;id 随时间单调增长,一段 id 也就对应一段时间。
// 末尾的 pmax 平时为空,只为本任务停摆太久时新行仍有处可去。
@Service
public class EventCallbackUrlPartitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventCallbackUrlPartitionService.class);

    static final String TABLE = "perf_event_callback_url";

    // 每段的 id 数,按 2026-09 的增速约一周。段越短,过期数据删得越及时
    static final long PARTITION_SIZE = 5_000_000L;

    // 当前正在写的那段之后,至少预留这么多段空分区
    static final int PARTITIONS_AHEAD = 2;

    // 回传最晚的是 14 日留存,将来可能有 30 日留存,再加上点击到激活的间隔
    static final Duration RETENTION = Duration.ofDays(45);

    // 还没分区的表不超过这么多行才原地转换。转换要整表拷贝,期间写入被挡住
    static final long CONVERTIBLE_ROWS = 100_000L;

    // DDL 要拿表的元数据锁,拿不到时后面的写入会排在它后面。等不到就放弃,下一轮再来
    private static final int LOCK_WAIT_SECONDS = 5;

    private static final String MAX_PARTITION = "pmax";

    record Partition(String name, Long upperBound) {
    }

    private final JdbcTemplate jdbcTemplate;

    public EventCallbackUrlPartitionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 3600000)
    public void maintain() {
        try {
            List<Partition> partitions = this.partitions();
            if (partitions.isEmpty()) {
                return;
            }

            long maxId = this.maxEventId();
            if (partitions.get(0).name() == null) {
                this.partitionTable(maxId);
                return;
            }

            this.extend(partitions, maxId);
            this.expire(partitions, maxId);
        } catch (RuntimeException e) {
            LOGGER.warn("Callback url partition maintenance failed", e);
        }
    }

    // 新环境里 Hibernate 按实体建出的是普通表,趁它还小转成分区表
    private void partitionTable(long maxId) {
        Long rows = this.jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (SELECT 1 FROM " + TABLE + " LIMIT " + (CONVERTIBLE_ROWS + 1) + ") t",
            Long.class);
        if ((rows == null) || (rows > CONVERTIBLE_ROWS)) {
            LOGGER.warn("Callback url table is not partitioned and too large to convert in place: rows>{}",
                CONVERTIBLE_ROWS);
            return;
        }

        List<Long> bounds = boundsToAdd(null, maxId);
        this.execute("ALTER TABLE " + TABLE + " PARTITION BY RANGE (event_id) (" + definitions(bounds) + ")");
        LOGGER.info("Callback url table partitioned: bounds={}", bounds);
    }

    private void extend(List<Partition> partitions, long maxId) {
        Partition last = partitions.get(partitions.size() - 1);
        Long highestBound = null;
        for (Partition partition : partitions) {
            if (partition.upperBound() != null) {
                highestBound = partition.upperBound();
            }
        }

        List<Long> bounds = boundsToAdd(highestBound, maxId);
        if (bounds.isEmpty()) {
            return;
        }

        if (last.upperBound() == null) {
            this.execute("ALTER TABLE " + TABLE + " REORGANIZE PARTITION " + last.name()
                + " INTO (" + definitions(bounds) + ")");
        } else {
            this.execute("ALTER TABLE " + TABLE + " ADD PARTITION (" + definitions(bounds) + ")");
        }
        LOGGER.info("Callback url partitions added: bounds={}", bounds);
    }

    // 从最老的一段往后删,遇到第一段还不能删的就停:id 越大事件越新,后面的只会更新
    private void expire(List<Partition> partitions, long maxId) {
        Instant now = Instant.now();
        for (Partition partition : partitions) {
            if ((partition.upperBound() == null) || !isClosed(partition.upperBound(), maxId)) {
                return;
            }

            Timestamp newest = this.newestEventTimeBelow(partition.upperBound());
            if (!isExpired(newest, now)) {
                return;
            }

            this.execute("ALTER TABLE " + TABLE + " DROP PARTITION " + partition.name());
            LOGGER.info("Callback url partition dropped: partition={}, newestEvent={}", partition.name(), newest);
        }
    }

    // 当前 id 所在的那段之后预留 PARTITIONS_AHEAD 段;返回要新增的各段上界,从 highestBound 之后接着排。
    // 还没有任何有限上界时,第一段从 0 起,把已有的行都收进去
    static List<Long> boundsToAdd(Long highestBound, long maxId) {
        long target = (maxId / PARTITION_SIZE + 1 + PARTITIONS_AHEAD) * PARTITION_SIZE;
        long start = (highestBound == null)
            ? (maxId / PARTITION_SIZE + 1) * PARTITION_SIZE
            : highestBound + PARTITION_SIZE;

        List<Long> bounds = new ArrayList<Long>();
        for (long bound = start; bound <= target; bound += PARTITION_SIZE) {
            bounds.add(bound);
        }
        return bounds;
    }

    // 上界以下的 id 已经全部发出,这段不会再有新行
    static boolean isClosed(long upperBound, long maxId) {
        return upperBound <= maxId;
    }

    // 一段里最新的事件都过了保留期,整段才算过期。找不到事件时保守地不删
    static boolean isExpired(Timestamp newestEventTime, Instant now) {
        return (newestEventTime != null) && newestEventTime.toInstant().isBefore(now.minus(RETENTION));
    }

    static String definitions(List<Long> bounds) {
        String definitions = bounds.stream()
            .map(bound -> "PARTITION p" + bound + " VALUES LESS THAN (" + bound + ")")
            .collect(Collectors.joining(", "));
        return definitions + ", PARTITION " + MAX_PARTITION + " VALUES LESS THAN MAXVALUE";
    }

    // 未分区的表也有一行,名字为 null;表不存在时没有行
    private List<Partition> partitions() {
        return this.jdbcTemplate.query("""
            SELECT PARTITION_NAME, PARTITION_DESCRIPTION
            FROM information_schema.PARTITIONS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            ORDER BY PARTITION_ORDINAL_POSITION
            """,
            (rs, rowNum) -> {
                String description = rs.getString("PARTITION_DESCRIPTION");
                Long upperBound = (description == null) || "MAXVALUE".equals(description)
                    ? null
                    : Long.valueOf(description);
                return new Partition(rs.getString("PARTITION_NAME"), upperBound);
            },
            TABLE);
    }

    // 走主键取最大值,实时准确;information_schema 里的 AUTO_INCREMENT 是缓存值,可能是一天前的
    private long maxEventId() {
        Long maxId = this.jdbcTemplate.queryForObject("SELECT MAX(id) FROM perf_event", Long.class);
        return maxId == null ? 0 : maxId;
    }

    private Timestamp newestEventTimeBelow(long upperBound) {
        List<Timestamp> times = this.jdbcTemplate.queryForList(
            "SELECT time FROM perf_event WHERE id < ? ORDER BY id DESC LIMIT 1", Timestamp.class, upperBound);
        return times.isEmpty() ? null : times.get(0);
    }

    // 连接会还回连接池,改过的会话变量要还原
    private void execute(String ddl) {
        this.jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET SESSION lock_wait_timeout = " + LOCK_WAIT_SECONDS);
                try {
                    statement.execute(ddl);
                } finally {
                    statement.execute("SET SESSION lock_wait_timeout = DEFAULT");
                }
            }
            return null;
        });
    }

}
