package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

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
@Table(name = "ad_vendor")
public class Vendor {

    public final static Integer PARTNER_TYPE_DIRECT       = 0;
    public final static Integer PARTNER_TYPE_PROGRAMMATIC = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonProperty(access = Access.WRITE_ONLY)
    private Tenant tenant;

    @OneToMany(mappedBy = "vendor", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VendorMedia> vendorMedia;

    @OneToMany(mappedBy = "vendor", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VendorPort> vendorPort;

    private String name;

    private Integer mode;

    private String ekey;

    private String ikey;

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

    public Tenant getTenant() {
        return this.tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public List<VendorMedia> getVendorMedia() {
        return this.vendorMedia;
    }

    public void setVendorMedia(List<VendorMedia> vendorMedia) {
        this.vendorMedia = vendorMedia;
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

    public Integer getMode() {
        return this.mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public String getEkey() {
        return this.ekey;
    }

    public void setEkey(String ekey) {
        this.ekey = ekey;
    }

    public String getIkey() {
        return this.ikey;
    }

    public void setIkey(String ikey) {
        this.ikey = ikey;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
