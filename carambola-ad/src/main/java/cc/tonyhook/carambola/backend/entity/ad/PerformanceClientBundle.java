package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class PerformanceClientBundle extends PerformanceClient {

    private String bundle;

    public PerformanceClientBundle() {
        super();
    }

    public PerformanceClientBundle(Integer clientPort, Integer vendorPort, String bundle, Timestamp time) {
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
