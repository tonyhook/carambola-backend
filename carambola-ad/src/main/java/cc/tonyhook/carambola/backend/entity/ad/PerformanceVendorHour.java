package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_performance_vendor_hour", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "time"),
})
public class PerformanceVendorHour extends PerformanceVendor {

    public PerformanceVendorHour() {
        super();
    }

    public PerformanceVendorHour(Integer clientPort, Integer vendorPort, Timestamp time) {
        super(clientPort, vendorPort, time);
    }

}
