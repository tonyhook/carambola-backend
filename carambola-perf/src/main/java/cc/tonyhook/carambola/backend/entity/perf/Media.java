package cc.tonyhook.carambola.backend.entity.perf;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "perf_media")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    private String name;

    private String code;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> protocolKey;

    // 该媒体回传签名所需的密钥项,渠道按这里的顺序填值。与 protocolKey 不同,
    // 密钥不参与渠道查找,也不会出现在监测链接里。
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> secretKey;

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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<String> getSecretKey() {
        return this.secretKey;
    }

    public void setSecretKey(List<String> secretKey) {
        this.secretKey = secretKey;
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
