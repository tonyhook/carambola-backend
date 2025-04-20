package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_vendor_media")
public class VendorMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonIgnoreProperties(value = {"tenant", "vendorMedia", "vendorPort", "createTime", "updateTime"}, allowSetters = true)
    private Vendor vendor;

    @OneToMany(mappedBy = "vendorMedia", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VendorPort> vendorPort;

    private String name;

    private String platform;

    private String apppackage;

    private String appversion;

    private String applink;

    @Lob
    @Column(length = 65536)
    private String remark;

    private Timestamp createTime;

    private Timestamp updateTime;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean isDeleted() {
        return this.deleted;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Vendor getVendor() {
        return this.vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public List<VendorPort> getVendorPort() {
        return this.vendorPort;
    }

    public void setVendorPort(List<VendorPort> vendorPort) {
        this.vendorPort = vendorPort;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getApppackage() {
        return this.apppackage;
    }

    public void setApppackage(String apppackage) {
        this.apppackage = apppackage;
    }

    public String getAppversion() {
        return this.appversion;
    }

    public void setAppversion(String appversion) {
        this.appversion = appversion;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getApplink() {
        return this.applink;
    }

    public void setApplink(String applink) {
        this.applink = applink;
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
