package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class PerformanceClient {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    protected Integer id;

    protected Integer clientPort;

    protected Integer vendorPort; // 0 for total

    protected Timestamp time;

    protected Long eventA;

    protected Long eventB;

    protected Long eventC;

    protected Long eventD;

    protected Long eventE;

    protected Long eventF;

    protected Long eventG;

    protected Long eventH;

    protected Long eventI;

    protected Long eventJ;

    protected Long eventK;

    protected Long eventL;

    protected Long eventM;

    protected Long eventN;

    protected Long eventO;

    protected Long impression;

    protected Long click;

    protected Long income;

    protected Long outcomeUpstream;

    protected Long outcomeRebate;

    protected Long outcomeDownstream;

    public PerformanceClient() {
        super();
    }

    public PerformanceClient(Integer clientPort, Integer vendorPort, Timestamp time) {
        this.clientPort = clientPort;
        this.vendorPort = vendorPort;
        this.time = time;
        this.eventA = 0L;
        this.eventB = 0L;
        this.eventC = 0L;
        this.eventD = 0L;
        this.eventE = 0L;
        this.eventF = 0L;
        this.eventG = 0L;
        this.eventH = 0L;
        this.eventI = 0L;
        this.eventJ = 0L;
        this.eventK = 0L;
        this.eventL = 0L;
        this.eventM = 0L;
        this.eventN = 0L;
        this.eventO = 0L;
        this.impression = 0L;
        this.click = 0L;
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

    public Timestamp getTime() {
        return this.time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public Long getEventA() {
        return this.eventA;
    }

    public void setEventA(Long eventA) {
        this.eventA = eventA;
    }

    public Long getEventB() {
        return this.eventB;
    }

    public void setEventB(Long eventB) {
        this.eventB = eventB;
    }

    public Long getEventC() {
        return this.eventC;
    }

    public void setEventC(Long eventC) {
        this.eventC = eventC;
    }

    public Long getEventD() {
        return this.eventD;
    }

    public void setEventD(Long eventD) {
        this.eventD = eventD;
    }

    public Long getEventE() {
        return this.eventE;
    }

    public void setEventE(Long eventE) {
        this.eventE = eventE;
    }

    public Long getEventF() {
        return this.eventF;
    }

    public void setEventF(Long eventF) {
        this.eventF = eventF;
    }

    public Long getEventG() {
        return this.eventG;
    }

    public void setEventG(Long eventG) {
        this.eventG = eventG;
    }

    public Long getEventH() {
        return this.eventH;
    }

    public void setEventH(Long eventH) {
        this.eventH = eventH;
    }

    public Long getEventI() {
        return this.eventI;
    }

    public void setEventI(Long eventI) {
        this.eventI = eventI;
    }

    public Long getEventJ() {
        return this.eventJ;
    }

    public void setEventJ(Long eventJ) {
        this.eventJ = eventJ;
    }

    public Long getEventK() {
        return this.eventK;
    }

    public void setEventK(Long eventK) {
        this.eventK = eventK;
    }

    public Long getEventL() {
        return this.eventL;
    }

    public void setEventL(Long eventL) {
        this.eventL = eventL;
    }

    public Long getEventM() {
        return this.eventM;
    }

    public void setEventM(Long eventM) {
        this.eventM = eventM;
    }

    public Long getEventN() {
        return this.eventN;
    }

    public void setEventN(Long eventN) {
        this.eventN = eventN;
    }

    public Long getEventO() {
        return this.eventO;
    }

    public void setEventO(Long eventO) {
        this.eventO = eventO;
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
