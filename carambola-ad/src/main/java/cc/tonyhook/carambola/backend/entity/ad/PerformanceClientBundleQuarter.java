package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_performance_client_bundle_quarter", indexes = {
    @Index(columnList = "clientPort"),
    @Index(columnList = "vendorPort"),
    @Index(columnList = "bundle"),
    @Index(columnList = "time"),
})
public class PerformanceClientBundleQuarter extends PerformanceClientBundle {

    public PerformanceClientBundleQuarter() {
        super();
    }

    public PerformanceClientBundleQuarter(Integer clientPort, Integer vendorPort, String bundle, Timestamp time) {
        super(clientPort, vendorPort, bundle, time);
    }

}
