package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;
import java.util.List;

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
@Table(name = "ad_vendor_port")
public class VendorPort {

    public final static Integer PORT_TYPE_SHARE   = 1;
    public final static Integer PORT_TYPE_BIDDING = 2;
    public final static Integer PORT_TYPE_DIRECT  = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonIgnoreProperties(value = {"tenant", "vendorMedia", "vendorPort", "createTime", "updateTime"}, allowSetters = true)
    private Vendor vendor;

    @ManyToOne
    @JsonIgnoreProperties(value = {"vendor", "vendorPort", "createTime", "updateTime"}, allowSetters = true)
    private VendorMedia vendorMedia;

    @OneToMany(mappedBy = "vendorPort", fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"connectionLog"}, allowSetters = true)
    private List<Connection> connection;

    private String name;

    private String format;

    private String tagId;

    private Integer mode;

    private Integer timeout;

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

    public VendorMedia getVendorMedia() {
        return this.vendorMedia;
    }

    public void setVendorMedia(VendorMedia vendorMedia) {
        this.vendorMedia = vendorMedia;
    }

    public List<Connection> getConnection() {
        return this.connection;
    }

    public void setConnection(List<Connection> connection) {
        this.connection = connection;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getTagId() {
        return this.tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public Integer getMode() {
        return this.mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public Integer getTimeout() {
        return this.timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
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
