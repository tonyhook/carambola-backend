package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Integer> {

    public List<Event> findByDirection(String direction);

    // 可相加的指标:次数、原始次数、金额、成本。userCount 恒为 0,去重人数由 aggregateUserCount 单算
    @Query(value = """
        SELECT
            DATE_FORMAT(
                TIMESTAMPADD(MINUTE, :timezoneOffset, e.time),
                IF(:interval = 'hour', '%Y-%m-%d %H:00:00', '%Y-%m-%d')
            ) AS period,
            e.client_channel_id AS clientChannelId,
            e.event AS eventName,
            SUM(CASE WHEN e.is_duplicate IS NULL OR e.is_duplicate = FALSE THEN 1 ELSE 0 END) AS count,
            COUNT(*) AS rawCount,
            SUM(CASE WHEN e.is_duplicate IS NULL OR e.is_duplicate = FALSE THEN e.amount ELSE NULL END) AS amount,
            SUM(CASE WHEN e.is_duplicate IS NULL OR e.is_duplicate = FALSE THEN e.cost ELSE NULL END) AS cost,
            0 AS userCount
        FROM perf_event e
        WHERE e.time >= :start
            AND e.time < :end
            AND e.client_channel_id IN (:clientChannelIds)
        GROUP BY period, e.client_channel_id, e.event
        """, nativeQuery = true)
    List<Object[]> aggregateTotals(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end,
            @Param("interval") String interval,
            @Param("timezoneOffset") Integer timezoneOffset,
            @Param("clientChannelIds") Collection<Integer> clientChannelIds);

    // 去重人数,列顺序与 aggregateTotals 相同、其余指标恒为 0。
    // 只扫计入人数的事件,靠的是它们都是漏斗末端的低频转化事件;把高频事件加进去这里会变慢
    @Query(value = """
        SELECT
            DATE_FORMAT(
                TIMESTAMPADD(MINUTE, :timezoneOffset, e.time),
                IF(:interval = 'hour', '%Y-%m-%d %H:00:00', '%Y-%m-%d')
            ) AS period,
            e.client_channel_id AS clientChannelId,
            e.event AS eventName,
            0 AS count,
            0 AS rawCount,
            NULL AS amount,
            NULL AS cost,
            COUNT(DISTINCT COALESCE(e.device_id, CONCAT('#', e.id))) AS userCount
        FROM perf_event e
        WHERE e.time >= :start
            AND e.time < :end
            AND e.client_channel_id IN (:clientChannelIds)
            AND e.event IN (:userCountEvents)
            AND (e.is_duplicate IS NULL OR e.is_duplicate = FALSE)
        GROUP BY period, e.client_channel_id, e.event
        """, nativeQuery = true)
    List<Object[]> aggregateUserCount(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end,
            @Param("interval") String interval,
            @Param("timezoneOffset") Integer timezoneOffset,
            @Param("clientChannelIds") Collection<Integer> clientChannelIds,
            @Param("userCountEvents") Collection<String> userCountEvents);

    // 按主键取最早一条,避免 time 上没有前导索引导致全表扫描
    @Query("SELECT e.time FROM Event e ORDER BY e.id LIMIT 1")
    Timestamp findFirstEventTime();

    // 同一个入口事件的所有回传都挂在它的 delivery 上,据此回溯它此前发生过的转化
    @Query("""
        SELECT MIN(c.time) FROM Event c
        WHERE c.eventDelivery.event.id = :eventId
            AND c.event IN :events
        """)
    Timestamp findFirstCallbackTime(
            @Param("eventId") Integer eventId,
            @Param("events") Collection<String> events);

    @Query(value = """
        SELECT e.*
        FROM perf_event e
        WHERE e.time >= :start
            AND e.time < :end
            AND e.event = :event
            AND e.client_channel_id IN (:clientChannelIds)
        ORDER BY e.time DESC, e.id DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Event> findRecentEvents(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end,
            @Param("event") String event,
            @Param("clientChannelIds") Collection<Integer> clientChannelIds,
            @Param("size") Integer size,
            @Param("offset") Integer offset);

    @Query(value = """
        SELECT COUNT(*)
        FROM perf_event e
        WHERE e.time >= :start
            AND e.time < :end
            AND e.event = :event
            AND e.client_channel_id IN (:clientChannelIds)
        """, nativeQuery = true)
    Long countRecentEvents(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end,
            @Param("event") String event,
            @Param("clientChannelIds") Collection<Integer> clientChannelIds);

}
