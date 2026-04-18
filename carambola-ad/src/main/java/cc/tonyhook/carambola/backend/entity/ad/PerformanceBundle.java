package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

public class PerformanceBundle {

    public final static String PERFORMANCE_NO_PROTOCOL                   = "A"; // -1 as client port
    public final static String PERFORMANCE_BAD_PROTOCOL_VER              = "B"; // -1 as client port
    public final static String PERFORMANCE_NO_ITEM                       = "C"; // -1 as client port
    public final static String PERFORMANCE_NOT_REGISTERED                = "D"; // -1 as client port
    public final static String PERFORMANCE_NO_MATCH_CONNECTION           = "E"; // -1 as client port
    public final static String PERFORMANCE_BEYOND_VENDOR_TRAFFIC_CONTROL = "F"; // -1 as client port
    public final static String PERFORMANCE_VENDOR_ANTI_FRAUD             = "G"; // -1 as client port
    public final static String PERFORMANCE_NO_RESPONSE                   = "H"; // -1 as client port
    public final static String PERFORMANCE_SHARE_SUCCESS                 = "I"; // one in each request
    public final static String PERFORMANCE_BIDDING_SUCCESS               = "J"; // one in each request

    // by connection, multiple in each request
    public final static String PERFORMANCE_TIMEOUT                       = "ZA";
    public final static String PERFORMANCE_REQUEST_FAILED                = "ZB";
    public final static String PERFORMANCE_NOT_BIDDING                   = "ZC";
    public final static String PERFORMANCE_SHARE_OK                      = "ZD";
    public final static String PERFORMANCE_BIDDING_OK                    = "ZE";
    public final static String PERFORMANCE_TRANS_FROM_FAILED             = "ZF";
    public final static String PERFORMANCE_TRANS_TO_FAILED               = "ZG";
    public final static String PERFORMANCE_UNKNOWN_CLIENT                = "ZH";
    public final static String PERFORMANCE_BIDDING_LOSE                  = "ZI";
    public final static String PERFORMANCE_BIDDING_WIN                   = "ZJ";
    public final static String PERFORMANCE_BIDDING_INVALID               = "ZK";
    public final static String PERFORMANCE_BEYOND_CLIENT_TRAFFIC_CONTROL = "ZL";
    public final static String PERFORMANCE_LOST_KEY_FIELD                = "ZM";
    public final static String PERFORMANCE_CLIENT_ANTI_FRAUD             = "ZN";
    public final static String PERFORMANCE_REQUEST_REJECTED              = "ZO";
    public final static String PERFORMANCE_FLOOR_INVALID                 = "ZP";

    public final static String TRACKING_IMPRESSION                       = "a";
    public final static String TRACKING_CLICK                            = "b";
    public final static String TRACKING_GENERAL                          = "z";

    private Integer clientPort;

    private Integer vendorPort;

    private String bundle;

    private Integer node;

    private Timestamp time;

    private String event;

    private Long amount;

    public PerformanceBundle() {
    }

    public PerformanceBundle(Integer clientPort, Integer vendorPort, String bundle, Integer node, Timestamp time, String event) {
        this.clientPort = clientPort;
        this.vendorPort = vendorPort;
        this.bundle = bundle;
        this.node = node;
        this.time = time;
        this.event = event;
        this.amount = 0L;
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

    public Integer getNode() {
        return this.node;
    }

    public void setNode(Integer node) {
        this.node = node;
    }

    public Timestamp getTime() {
        return this.time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getEvent() {
        return this.event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getAmount() {
        return this.amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

}
