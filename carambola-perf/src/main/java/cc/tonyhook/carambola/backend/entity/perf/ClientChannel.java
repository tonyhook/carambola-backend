package cc.tonyhook.carambola.backend.entity.perf;

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "perf_client_channel",
    indexes = {
        @Index(name = "idx_perf_client_channel_media_code", columnList = "media_name, media_code")
    }
)
public class ClientChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonIgnoreProperties(value = {"clientProject", "clientChannel", "tenant", "createTime", "updateTime"}, allowSetters = true)
    private Client client;

    @ManyToOne
    @JsonIgnoreProperties(value = {"client", "clientChannel", "createTime", "updateTime"}, allowSetters = true)
    private ClientProject clientProject;

    private String mediaName;

    private String mediaCode;

    // 媒体方密钥,按 Media.secretKey 声明的顺序以竖线拼接。与 mediaCode 不同,
    // 它只用于回传签名,不会出现在监测链接里。留空表示该媒体不需要密钥。
    @Column(length = 512)
    private String mediaSecret;

    @Column(name = "operator_name")
    private String operator;

    private Boolean filterInvalidId;

    // 单价,单位为分。留空表示该渠道不计费。
    @Column(precision = 18, scale = 2)
    private BigDecimal cpc;

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

    public Client getClient() {
        return this.client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ClientProject getClientProject() {
        return this.clientProject;
    }

    public void setClientProject(ClientProject clientProject) {
        this.clientProject = clientProject;
    }

    public String getMediaName() {
        return this.mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getMediaCode() {
        return this.mediaCode;
    }

    public void setMediaCode(String mediaCode) {
        this.mediaCode = mediaCode;
    }

    public String getMediaSecret() {
        return this.mediaSecret;
    }

    public void setMediaSecret(String mediaSecret) {
        this.mediaSecret = mediaSecret;
    }

    public String getOperator() {
        return this.operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Boolean getFilterInvalidId() {
        return this.filterInvalidId;
    }

    public Boolean isFilterInvalidId() {
        return this.filterInvalidId;
    }

    public void setFilterInvalidId(Boolean filterInvalidId) {
        this.filterInvalidId = filterInvalidId;
    }

    public BigDecimal getCpc() {
        return this.cpc;
    }

    public void setCpc(BigDecimal cpc) {
        this.cpc = cpc;
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
