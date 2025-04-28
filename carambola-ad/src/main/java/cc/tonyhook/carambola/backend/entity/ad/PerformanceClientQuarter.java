package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_performance_client_quarter", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "time"),
})
public class PerformanceClientQuarter extends PerformanceClient {

    public PerformanceClientQuarter() {
        super();
    }

    public PerformanceClientQuarter(Integer clientPort, Integer vendorPort, Timestamp time) {
        super(clientPort, vendorPort, time);
    }

}
