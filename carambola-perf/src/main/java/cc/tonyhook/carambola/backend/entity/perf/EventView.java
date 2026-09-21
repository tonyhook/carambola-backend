package cc.tonyhook.carambola.backend.entity.perf;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

public class EventView {

    private Integer id;

    private Timestamp time;

    private String event;

    private String eventName;

    private BigDecimal amount;

    private String media;

    private String mediaCode;

    private String operator;

    private Boolean forwarded;

    private Boolean forwardSucceeded;

    private Map<String, String> queries;

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

    public String getEvent() {
        return this.event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEventName() {
        return this.eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getOperator() {
        return this.operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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

    public Map<String, String> getQueries() {
        return this.queries;
    }

    public void setQueries(Map<String, String> queries) {
        this.queries = queries;
    }

}
