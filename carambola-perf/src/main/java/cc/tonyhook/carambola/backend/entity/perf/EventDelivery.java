package cc.tonyhook.carambola.backend.entity.perf;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "perf_event_delivery",
    indexes = {
        @Index(name = "uk_perf_event_delivery_token", columnList = "token", unique = true),
        @Index(name = "idx_perf_event_delivery_event", columnList = "event_id"),
        @Index(name = "idx_perf_event_delivery_route", columnList = "route_id")
    }
)
public class EventDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "event_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_perf_event_delivery_event")
    )
    private Event event;

    @ManyToOne
    @JoinColumn(
        name = "route_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_perf_event_delivery_route")
    )
    private ClientChannelRoute route;

    private String token;

    private String trackName;

    private String trackCode;

    private Boolean forwarded;

    private Boolean succeeded;

    private Timestamp createTime;

    private Timestamp updateTime;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return this.event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public ClientChannelRoute getRoute() {
        return this.route;
    }

    public void setRoute(ClientChannelRoute route) {
        this.route = route;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTrackName() {
        return this.trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getTrackCode() {
        return this.trackCode;
    }

    public void setTrackCode(String trackCode) {
        this.trackCode = trackCode;
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

    public Boolean getSucceeded() {
        return this.succeeded;
    }

    public Boolean isSucceeded() {
        return this.succeeded;
    }

    public void setSucceeded(Boolean succeeded) {
        this.succeeded = succeeded;
    }

    public Timestamp getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

}
