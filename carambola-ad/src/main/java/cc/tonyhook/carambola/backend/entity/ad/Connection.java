package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_connection")
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonIgnoreProperties(value = {"client", "clientMedia", "connection", "createTime", "updateTime"}, allowSetters = true)
    private ClientPort clientPort;

    @ManyToOne
    @JsonIgnoreProperties(value = {"vendor", "vendorMedia", "connection", "createTime", "updateTime"}, allowSetters = true)
    private VendorPort vendorPort;

    @OneToMany(mappedBy = "connection", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ConnectionLog> connectionLog;

    private Integer priority;

    private Boolean enabled;

    private Boolean test;

    private Timestamp validFrom;

    private Timestamp validTo;

    private Double upstreamRatio;

    private Double rebateRatio;

    private Double platformRatio;

    private Double downstreamRatio;

    private Integer defaultPrice;

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

    public ClientPort getClientPort() {
        return this.clientPort;
    }

    public void setClientPort(ClientPort clientPort) {
        this.clientPort = clientPort;
    }

    public VendorPort getVendorPort() {
        return this.vendorPort;
    }

    public void setVendorPort(VendorPort vendorPort) {
        this.vendorPort = vendorPort;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isTest() {
        return this.test;
    }

    public Boolean getTest() {
        return this.test;
    }

    public void setTest(Boolean test) {
        this.test = test;
    }

    public Timestamp getValidFrom() {
        return this.validFrom;
    }

    public void setValidFrom(Timestamp validFrom) {
        this.validFrom = validFrom;
    }

    public Timestamp getValidTo() {
        return this.validTo;
    }

    public void setValidTo(Timestamp validTo) {
        this.validTo = validTo;
    }

    public Double getUpstreamRatio() {
        return this.upstreamRatio;
    }

    public void setUpstreamRatio(Double upstreamRatio) {
        this.upstreamRatio = upstreamRatio;
    }

    public Double getRebateRatio() {
        return this.rebateRatio;
    }

    public void setRebateRatio(Double rebateRatio) {
        this.rebateRatio = rebateRatio;
    }

    public Double getPlatformRatio() {
        return this.platformRatio;
    }

    public void setPlatformRatio(Double platformRatio) {
        this.platformRatio = platformRatio;
    }

    public Double getDownstreamRatio() {
        return this.downstreamRatio;
    }

    public void setDownstreamRatio(Double downstreamRatio) {
        this.downstreamRatio = downstreamRatio;
    }

    public Integer getDefaultPrice() {
        return this.defaultPrice;
    }

    public void setDefaultPrice(Integer defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public List<ConnectionLog> getConnectionLog() {
        return this.connectionLog;
    }

    public void setConnectionLog(List<ConnectionLog> connectionLog) {
        this.connectionLog = connectionLog;
    }

}
