package cc.tonyhook.carambola.backend.entity.perf;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(
    name = "perf_event",
    indexes = {
        @Index(name = "idx_perf_event_count", columnList = "client_channel_id, time, event, forwarded"),
        @Index(name = "idx_perf_event_count_metrics", columnList = "client_channel_id, time, event, is_duplicate, amount, cost, device_id"),
        @Index(name = "idx_perf_event_recent", columnList = "client_channel_id, event, time, id"),
        @Index(name = "idx_perf_event_recent_order", columnList = "event, time, id, client_channel_id"),
        @Index(name = "idx_perf_event_delivery_id", columnList = "event_delivery_id")
    }
)
public class Event {

    public static final String DIRECTION_UPSTREAM = "upstream";

    public static final String DIRECTION_DOWNSTREAM = "downstream";

    // 回传地址。它只在回传窗口内有用,单独存在 perf_event_callback_url 里按保留期滚动删除,不进 queries 列
    public static final String CALLBACK_QUERY = "davidia_callback";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Timestamp time;

    private String direction;

    @Transient
    private String media;

    @Transient
    private String mediaCode;

    @ManyToOne
    private ClientChannel clientChannel;

    private String event;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    // 成交价快照,单位为分。仅点击事件计费,写入后不随渠道改价而变动。
    @Column(precision = 18, scale = 2)
    private BigDecimal cost;

    // 设备标识,来源于入口事件的设备号;回传事件沿用其归因入口事件的标识。
    @Column(name = "device_id", length = 128)
    private String deviceId;

    private Boolean forwarded;

    private Boolean forwardSucceeded;

    @Column(name = "is_duplicate")
    private Boolean duplicate;

    @ManyToOne
    @JoinColumn(
        name = "event_delivery_id",
        foreignKey = @ForeignKey(name = "fk_perf_event_delivery_id")
    )
    private EventDelivery eventDelivery;

    // 处理器看到的完整参数,带着回传地址
    @Transient
    private Map<String, String> queries;

    // 落库的参数,不含回传地址。只在插入时从 queries 生成,插入后再改 queries 不会落库;
    // 早于侧表上线的旧行里仍带着回传地址,加载后照样出现在 queries 里
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "queries")
    private Map<String, String> storedQueries;

    @PrePersist
    void storeQueries() {
        if (this.queries == null) {
            this.storedQueries = null;
            return;
        }

        this.storedQueries = new LinkedHashMap<String, String>(this.queries);
        this.storedQueries.remove(CALLBACK_QUERY);
    }

    @PostLoad
    void loadQueries() {
        this.queries = this.storedQueries == null ? null : new LinkedHashMap<String, String>(this.storedQueries);
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getTime() {
        return this.time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getDirection() {
        return this.direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getMedia() {
        return this.media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public String getMediaCode() {
        return this.mediaCode;
    }

    public void setMediaCode(String mediaCode) {
        this.mediaCode = mediaCode;
    }

    public ClientChannel getClientChannel() {
        return this.clientChannel;
    }

    public void setClientChannel(ClientChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public String getEvent() {
        return this.event;
    }

    public void setEvent(String event) {
        this.event = event;
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

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Boolean getForwarded() {
        return this.forwarded;
    }

    public Boolean isForwarded() {
        return this.forwarded;
    }

    public void setForwarded(Boolean forwarded) {
        this.forwarded = forwarded;
    }

    public Boolean getForwardSucceeded() {
        return this.forwardSucceeded;
    }

    public Boolean isForwardSucceeded() {
        return this.forwardSucceeded;
    }

    public void setForwardSucceeded(Boolean forwardSucceeded) {
        this.forwardSucceeded = forwardSucceeded;
    }

    public Boolean getDuplicate() {
        return this.duplicate;
    }

    public Boolean isDuplicate() {
        return this.duplicate;
    }

    public void setDuplicate(Boolean duplicate) {
        this.duplicate = duplicate;
    }

    public EventDelivery getEventDelivery() {
        return this.eventDelivery;
    }

    public void setEventDelivery(EventDelivery eventDelivery) {
        this.eventDelivery = eventDelivery;
    }

    public Map<String,String> getQueries() {
        return this.queries;
    }

    public void setQueries(Map<String,String> queries) {
        this.queries = queries;
    }

}
