package cc.tonyhook.carambola.backend.dao.perf;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.perf.EventHourly;

public interface EventHourlyRepository extends JpaRepository<EventHourly, Integer> {

    @Modifying
    @Query("DELETE FROM EventHourly h WHERE h.hourStart = :hourStart")
    void deleteByHourStart(@Param("hourStart") Timestamp hourStart);

    // 列顺序与 EventRepository.aggregateTotals 相同;start、end 须落在 UTC 整点上
    @Query(value = """
        SELECT
            DATE_FORMAT(
                TIMESTAMPADD(MINUTE, :timezoneOffset, h.hour_start),
                IF(:interval = 'hour', '%Y-%m-%d %H:00:00', '%Y-%m-%d')
            ) AS period,
            h.client_channel_id AS clientChannelId,
            h.event AS eventName,
            SUM(h.event_count) AS count,
            SUM(h.raw_count) AS rawCount,
            SUM(h.amount) AS amount,
            SUM(h.cost) AS cost,
            0 AS userCount
        FROM perf_event_hourly h
        WHERE h.hour_start >= :start
            AND h.hour_start < :end
            AND h.client_channel_id IN (:clientChannelIds)
        GROUP BY period, h.client_channel_id, h.event
        """, nativeQuery = true)
    List<Object[]> aggregate(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end,
            @Param("interval") String interval,
            @Param("timezoneOffset") Integer timezoneOffset,
            @Param("clientChannelIds") Collection<Integer> clientChannelIds);

}
