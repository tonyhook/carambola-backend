package cc.tonyhook.carambola.backend.entity.ad;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_traffic_control", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "bundle"),
})
public class TrafficControl {

    public static final int TC_INDICATOR_REQUEST = 1;
    public static final int TC_INDICATOR_COST    = 2;

    public static final int TC_PERIOD_SECOND = 1;
    public static final int TC_PERIOD_MINUTE = 2;
    public static final int TC_PERIOD_HOUR   = 3;
    public static final int TC_PERIOD_DAY    = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer clientPort;

    private Integer vendorPort;

    private String bundle;

    private Integer indicator;

    private Integer period;

    private Long limitation;

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

    public Integer getIndicator() {
        return this.indicator;
    }

    public void setIndicator(Integer indicator) {
        this.indicator = indicator;
    }

    public Integer getPeriod() {
        return this.period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Long getLimitation() {
        return this.limitation;
    }

    public void setLimitation(Long limitation) {
        this.limitation = limitation;
    }

}
