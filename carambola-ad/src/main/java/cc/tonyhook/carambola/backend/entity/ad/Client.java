package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "ad_client")
public class Client {

    public final static Integer PARTNER_TYPE_DIRECT       = 0;
    public final static Integer PARTNER_TYPE_PROGRAMMATIC = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonProperty(access = Access.WRITE_ONLY)
    private Tenant tenant;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ClientMedia> clientMedia;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ClientPort> clientPort;

    private String name;

    private Integer mode;

    private String code;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> protocolKey;

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

    public List<ClientMedia> getClientMedia() {
        return this.clientMedia;
    }

    public void setClientMedia(List<ClientMedia> clientMedia) {
        this.clientMedia = clientMedia;
    }

    public List<ClientPort> getClientPort() {
        return this.clientPort;
    }

    public void setClientPort(List<ClientPort> clientPort) {
        this.clientPort = clientPort;
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

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getProtocolKey() {
        return this.protocolKey;
    }

    public void setProtocolKey(List<String> protocolKey) {
        this.protocolKey = protocolKey;
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
