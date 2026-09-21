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
    name = "perf_client_channel_route",
    indexes = {
        @Index(name = "idx_perf_channel_route_channel_event", columnList = "client_channel_id, event, deleted")
    }
)
public class ClientChannelRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(
        name = "client_channel_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_perf_channel_route_channel")
    )
    private ClientChannel clientChannel;

    private String event;

    private String trackName;

    private String trackCode;

    private Boolean deleted;

    private Timestamp createTime;

    private Timestamp updateTime;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Boolean getDeleted() {
        return this.deleted;
    }

    public Boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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
