package cc.tonyhook.carambola.backend.entity.perf;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CountView {

    private String time;

    private Integer clientId;

    private String clientName;

    private Integer clientProjectId;

    private String clientProjectName;

    private String media;

    private String mediaCode;

    private String operator;

    private Integer clientChannelId;

    // 实际花费,单位为分,来自点击事件的成交价快照。
    private BigDecimal spend = BigDecimal.ZERO;

    private Map<String, Long> eventCounts = new HashMap<String, Long>();

    private Map<String, Long> eventUserCounts = new HashMap<String, Long>();

    private Map<String, Long> eventRawCounts = new HashMap<String, Long>();

    private Map<String, BigDecimal> eventAmounts = new HashMap<String, BigDecimal>();

    private Map<String, String> eventNames = new HashMap<String, String>();

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getClientId() {
        return this.clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return this.clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Integer getClientProjectId() {
        return this.clientProjectId;
    }

    public void setClientProjectId(Integer clientProjectId) {
        this.clientProjectId = clientProjectId;
    }

    public String getClientProjectName() {
        return this.clientProjectName;
    }

    public void setClientProjectName(String clientProjectName) {
        this.clientProjectName = clientProjectName;
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

    public Integer getClientChannelId() {
        return this.clientChannelId;
    }

    public void setClientChannelId(Integer clientChannelId) {
        this.clientChannelId = clientChannelId;
    }

    public BigDecimal getSpend() {
        return this.spend;
    }

    public void setSpend(BigDecimal spend) {
        this.spend = spend;
    }

    public Map<String, Long> getEventCounts() {
        return this.eventCounts;
    }

    public void setEventCounts(Map<String, Long> eventCounts) {
        this.eventCounts = eventCounts;
    }

    public Map<String, Long> getEventUserCounts() {
        return this.eventUserCounts;
    }

    public void setEventUserCounts(Map<String, Long> eventUserCounts) {
        this.eventUserCounts = eventUserCounts;
    }

    public Map<String, Long> getEventRawCounts() {
        return this.eventRawCounts;
    }

    public void setEventRawCounts(Map<String, Long> eventRawCounts) {
        this.eventRawCounts = eventRawCounts;
    }

    public Map<String, BigDecimal> getEventAmounts() {
        return this.eventAmounts;
    }

    public void setEventAmounts(Map<String, BigDecimal> eventAmounts) {
        this.eventAmounts = eventAmounts;
    }

    public Map<String, String> getEventNames() {
        return this.eventNames;
    }

    public void setEventNames(Map<String, String> eventNames) {
        this.eventNames = eventNames;
    }

}
