package cc.tonyhook.carambola.backend.entity.perf;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "perf_event_catalog",
    indexes = {
        @Index(name = "idx_perf_event_catalog_event", columnList = "event", unique = true)
    }
)
public class EventCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted = false;

    private String category;

    private String event;

    private String name;

    @Column(nullable = false)
    private Boolean pairingEvent = false;

    @Column(nullable = false)
    private Boolean defaultMediaEvent = false;

    private Timestamp createTime;

    private Timestamp updateTime;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean isDeleted() {
        return this.deleted;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEvent() {
        return this.event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getPairingEvent() {
        return this.pairingEvent;
    }

    public Boolean isPairingEvent() {
        return this.pairingEvent;
    }

    public void setPairingEvent(Boolean pairingEvent) {
        this.pairingEvent = pairingEvent;
    }

    public Boolean getDefaultMediaEvent() {
        return this.defaultMediaEvent;
    }

    public Boolean isDefaultMediaEvent() {
        return this.defaultMediaEvent;
    }

    public void setDefaultMediaEvent(Boolean defaultMediaEvent) {
        this.defaultMediaEvent = defaultMediaEvent;
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
