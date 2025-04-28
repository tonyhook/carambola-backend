package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_finance_bundle", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "time"),
})
public class FinanceBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer clientPort;

    private Integer vendorPort;

    private String bundle;

    private Integer node;

    private Timestamp time;

    private Long income;

    private Long outcomeUpstream;

    private Long outcomeRebate;

    private Long outcomeDownstream;

    public FinanceBundle() {
    }

    public FinanceBundle(Integer clientPort, Integer vendorPort, String bundle, Integer node, Timestamp time) {
        this.clientPort = clientPort;
        this.vendorPort = vendorPort;
        this.bundle = bundle;
        this.node = node;
        this.time = time;
        this.income = 0L;
        this.outcomeUpstream = 0L;
        this.outcomeRebate = 0L;
        this.outcomeDownstream = 0L;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getBundle() {
        return this.bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public Integer getNode() {
        return this.node;
    }

    public void setNode(Integer node) {
        this.node = node;
    }

    public Timestamp getTime() {
        return this.time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
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
