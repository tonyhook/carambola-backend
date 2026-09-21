package cc.tonyhook.carambola.backend.entity.perf;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

// perf_event 按 UTC 整点、渠道、事件预先汇总的结果,口径与明细聚合一致。
// 只存可相加的指标;去重人数不能跨小时相加,仍从明细现算。
@Entity
@Table(
    name = "perf_event_hourly",
    indexes = {
        @Index(name = "uk_perf_event_hourly", columnList = "hour_start, client_channel_id, event", unique = true)
    }
)
public class EventHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hour_start")
    private Timestamp hourStart;

    @Column(name = "client_channel_id")
    private Integer clientChannelId;

    private String event;

    // 去掉重复事件后的条数
    private Long eventCount;

    private Long rawCount;

    @Column(precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(precision = 20, scale = 2)
    private BigDecimal cost;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getHourStart() {
        return this.hourStart;
    }

    public void setHourStart(Timestamp hourStart) {
        this.hourStart = hourStart;
    }

    public Integer getClientChannelId() {
        return this.clientChannelId;
    }

    public void setClientChannelId(Integer clientChannelId) {
        this.clientChannelId = clientChannelId;
    }

    public String getEvent() {
        return this.event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getEventCount() {
        return this.eventCount;
    }

    public void setEventCount(Long eventCount) {
        this.eventCount = eventCount;
    }

    public Long getRawCount() {
        return this.rawCount;
    }

    public void setRawCount(Long rawCount) {
        this.rawCount = rawCount;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCost() {
        return this.cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

}
