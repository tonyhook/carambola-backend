package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"tenant", "clientMedia", "clientPort", "createTime", "updateTime"}, allowSetters = true)
    private List<Client> client;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"tenant", "vendorMedia", "vendorPort", "createTime", "updateTime"}, allowSetters = true)
    private List<Vendor> vendor;

    private String name;

    private Boolean enabled;

    private Timestamp createTime;

    private Timestamp updateTime;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"tenant"}, allowSetters = true)
    private List<TenantUser> user;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Client> getClient() {
        return this.client;
    }

    public void setClient(List<Client> client) {
        this.client = client;
    }

    public List<Vendor> getVendor() {
        return this.vendor;
    }

    public void setVendor(List<Vendor> vendor) {
        this.vendor = vendor;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    public List<TenantUser> getUser() {
        return this.user;
    }

    public void setUser(List<TenantUser> user) {
        this.user = user;
    }

}
