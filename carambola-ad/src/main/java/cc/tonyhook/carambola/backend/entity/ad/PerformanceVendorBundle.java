package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class PerformanceVendorBundle extends PerformanceVendor {

    private String bundle;

    public PerformanceVendorBundle() {
        super();
    }

    public PerformanceVendorBundle(Integer clientPort, Integer vendorPort, String bundle, Timestamp time) {
        super(clientPort, vendorPort, time);
        this.bundle = bundle;
    }

    public String getBundle() {
        return this.bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

}
