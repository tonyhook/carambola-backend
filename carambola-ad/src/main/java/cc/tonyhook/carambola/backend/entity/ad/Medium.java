package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_medium", indexes = {
    @Index(columnList = "date"),
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
})
public class Medium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Timestamp date;

    private Integer clientPort;

    private Integer vendorPort;

    private Long request;

    private Long response;

    private Long impression;

    private Long click;

    private Long income;

    private Long outcomeUpstream;

    private Long outcomeRebate;

    private Long outcomeDownstream;

    public Medium() {
        super();
    }

    public Medium(Integer clientPort, Integer vendorPort, Timestamp date) {
        this.clientPort = clientPort;
        this.vendorPort = vendorPort;
        this.date = date;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getDate() {
        return this.date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
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

    public Long getResponse() {
        return this.response;
    }

    public void setResponse(Long response) {
        this.response = response;
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
