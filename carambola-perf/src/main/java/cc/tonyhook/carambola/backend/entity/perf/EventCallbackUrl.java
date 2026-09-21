package cc.tonyhook.carambola.backend.entity.perf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 入口事件的回传地址,约占原先整行的六成。只在回传窗口内有用,所以不放在 perf_event 里,
// 单独成表后按保留期整段删除。没有外键:按保留期滚动的分区表不支持外键
@Entity
@Table(name = "perf_event_callback_url")
public class EventCallbackUrl {

    @Id
    private Integer eventId;

    @Column(columnDefinition = "text")
    private String url;

    public EventCallbackUrl() {
    }

    public EventCallbackUrl(Integer eventId, String url) {
        this.eventId = eventId;
        this.url = url;
    }

    public Integer getEventId() {
        return this.eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
