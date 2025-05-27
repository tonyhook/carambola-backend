package cc.tonyhook.carambola.backend.entity.ad;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_anti_fraud", indexes = {
    @Index(columnList = "clientPort"),
})
public class AntiFraud {

    public static final int AF_PERIOD_SECOND = 1;
    public static final int AF_PERIOD_MINUTE = 2;
    public static final int AF_PERIOD_HOUR   = 3;
    public static final int AF_PERIOD_DAY    = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer clientPort;

    private String rule;

    private Integer period;

    private Double limitation;

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

    public String getRule() {
        return this.rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Integer getPeriod() {
        return this.period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Double getLimitation() {
        return this.limitation;
    }

    public void setLimitation(Double limitation) {
        this.limitation = limitation;
    }

}
