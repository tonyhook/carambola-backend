package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

public class PerformanceBundleView extends PerformanceView {

    private String bundle;

    public PerformanceBundleView() {
        super();
    }

    public PerformanceBundleView(Integer clientPort, Integer vendorPort, String bundle, Timestamp time) {
        super();
        this.bundle = bundle;
    }

    public String getBundle() {
        return this.bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

}
