package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_server")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String domain;

    private String address;

    private Integer node;

    private String username;

    private String password;

    private Timestamp servingTimestamp;

    private String servingVersion;

    private Timestamp trackingTimestamp;

    private String trackingVersion;

    private Timestamp updateTimestamp;

    private Timestamp createTime;

    private Timestamp updateTime;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getNode() {
        return this.node;
    }

    public void setNode(Integer node) {
        this.node = node;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getServingTimestamp() {
        return this.servingTimestamp;
    }

    public void setServingTimestamp(Timestamp servingTimestamp) {
        this.servingTimestamp = servingTimestamp;
    }

    public String getServingVersion() {
        return this.servingVersion;
    }

    public void setServingVersion(String servingVersion) {
        this.servingVersion = servingVersion;
    }

    public Timestamp getTrackingTimestamp() {
        return this.trackingTimestamp;
    }

    public void setTrackingTimestamp(Timestamp trackingTimestamp) {
        this.trackingTimestamp = trackingTimestamp;
    }

    public String getTrackingVersion() {
        return this.trackingVersion;
    }

    public void setTrackingVersion(String trackingVersion) {
        this.trackingVersion = trackingVersion;
    }

    public Timestamp getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public void setUpdateTimestamp(Timestamp updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public Timestamp getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

}
