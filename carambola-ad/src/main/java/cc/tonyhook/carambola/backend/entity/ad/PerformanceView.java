package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

public class PerformanceView {

    private String time;

    private Timestamp start;

    private Timestamp end;

    private Integer client;

    private Integer vendor;

    private Integer clientMedia;

    private Integer vendorMedia;

    private Integer clientPort;

    private Integer vendorPort;

    private Long request;

    private Long requestv;

    private Long response;

    private Long responsev;

    private Long impression;

    private Long click;

    private Long income;

    private Long outcomeUpstream;

    private Long outcomeRebate;

    private Long outcomeDownstream;

    public PerformanceView() {
        this.time = "";
        this.start = null;
        this.end = null;
        this.client = -1;
        this.vendor = -1;
        this.clientMedia = -1;
        this.vendorMedia = -1;
        this.clientPort = -1;
        this.vendorPort = -1;
        this.request = 0L;
        this.response = 0L;
        this.requestv = 0L;
        this.responsev = 0L;
        this.impression = 0L;
        this.click = 0L;
        this.income = 0L;
        this.outcomeUpstream = 0L;
        this.outcomeRebate = 0L;
        this.outcomeDownstream = 0L;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Timestamp getStart() {
        return this.start;
    }

    public void setStart(Timestamp start) {
        this.start = start;
    }

    public Timestamp getEnd() {
        return this.end;
    }

    public void setEnd(Timestamp end) {
        this.end = end;
    }

    public Integer getClient() {
        return this.client;
    }

    public void setClient(Integer client) {
        this.client = client;
    }

    public Integer getVendor() {
        return this.vendor;
    }

    public void setVendor(Integer vendor) {
        this.vendor = vendor;
    }

    public Integer getClientMedia() {
        return this.clientMedia;
    }

    public void setClientMedia(Integer clientMedia) {
        this.clientMedia = clientMedia;
    }

    public Integer getVendorMedia() {
        return this.vendorMedia;
    }

    public void setVendorMedia(Integer vendorMedia) {
        this.vendorMedia = vendorMedia;
    }

    public Integer getClientPort() {
        return this.clientPort;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public Integer getVendorPort() {
        return this.vendorPort;
    }

    public void setVendorPort(Integer vendorPort) {
        this.vendorPort = vendorPort;
    }

    public Long getRequest() {
        return this.request;
    }

    public void setRequest(Long request) {
        this.request = request;
    }

    public Long getRequestv() {
        return this.requestv;
    }

    public void setRequestv(Long requestv) {
        this.requestv = requestv;
    }

    public Long getResponse() {
        return this.response;
    }

    public void setResponse(Long response) {
        this.response = response;
    }

    public Long getResponsev() {
        return this.responsev;
    }

    public void setResponsev(Long responsev) {
        this.responsev = responsev;
    }

    public Long getImpression() {
        return this.impression;
    }

    public void setImpression(Long impression) {
        this.impression = impression;
    }

    public Long getClick() {
        return this.click;
    }

    public void setClick(Long click) {
        this.click = click;
    }

    public Long getIncome() {
        return this.income;
    }

    public void setIncome(Long income) {
        this.income = income;
    }

    public Long getOutcomeUpstream() {
        return this.outcomeUpstream;
    }

    public void setOutcomeUpstream(Long outcomeUpstream) {
        this.outcomeUpstream = outcomeUpstream;
    }

    public Long getOutcomeRebate() {
        return this.outcomeRebate;
    }

    public void setOutcomeRebate(Long outcomeRebate) {
        this.outcomeRebate = outcomeRebate;
    }

    public Long getOutcomeDownstream() {
        return this.outcomeDownstream;
    }

    public void setOutcomeDownstream(Long outcomeDownstream) {
        this.outcomeDownstream = outcomeDownstream;
    }

}
