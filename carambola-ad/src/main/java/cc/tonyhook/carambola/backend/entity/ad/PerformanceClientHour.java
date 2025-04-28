package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_performance_client_hour", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "time"),
})
public class PerformanceClientHour extends PerformanceClient {

    public PerformanceClientHour() {
        super();
    }

    public PerformanceClientHour(Integer clientPort, Integer vendorPort, Timestamp time) {
        super(clientPort, vendorPort, time);
    }

}
