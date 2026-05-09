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
@Table(name = "ad_client_port")
public class ClientPort {

    public final static Integer PORT_TYPE_SHARE   = 1;
    public final static Integer PORT_TYPE_BIDDING = 2;
    public final static Integer PORT_TYPE_DIRECT  = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean deleted;

    @ManyToOne
    @JsonIgnoreProperties(value = {"tenant", "clientMedia", "clientPort", "createTime", "updateTime"}, allowSetters = true)
    private Client client;

    @ManyToOne
    @JsonIgnoreProperties(value = {"client", "clientPort", "createTime", "updateTime"}, allowSetters = true)
    private ClientMedia clientMedia;

    @OneToMany(mappedBy = "clientPort", fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"connectionLog"}, allowSetters = true)
    private List<Connection> connection;

    private String name;

    private String format;

    private String tagId;

    private Integer mode;

    private String ekey;

    private String ikey;

    private String appname;

    private String apppackage;

    @Lob
    @Column(length = 65536)
    private String filter;

    private String filterType;

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

    public ClientMedia getClientMedia() {
        return this.clientMedia;
    }

    public void setClientMedia(ClientMedia clientMedia) {
        this.clientMedia = clientMedia;
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

    public String getAppname() {
        return this.appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getApppackage() {
        return this.apppackage;
    }

    public void setApppackage(String apppackage) {
        this.apppackage = apppackage;
    }

    public String getFilter() {
        return this.filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilterType() {
        return this.filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
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
