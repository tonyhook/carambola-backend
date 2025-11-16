package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_performance_vendor_bundle_minute", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "bundle"),
    @Index(columnList = "time"),
})
public class PerformanceVendorBundleMinute extends PerformanceVendorBundle {

    public PerformanceVendorBundleMinute() {
        super();
    }

    public PerformanceVendorBundleMinute(Integer clientPort, Integer vendorPort, String bundle, Timestamp time) {
        super(clientPort, vendorPort, bundle, time);
    }

}
