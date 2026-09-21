package cc.tonyhook.carambola.backend.entity.perf;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "perf_tenant_user")
public class TenantUser {

    public static final int ROLE_TENANT_MANAGER         = 1 << 0 << 0;
    public static final int ROLE_CLIENT_MANAGER         = 1 << 0 << 12;
    public static final int ROLE_CLIENT_PROJECT_MANAGER = 1 << 0 << 16;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private Tenant tenant;

    private String username;

    private Integer role;

    private Integer resource;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return this.tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRole() {
        return this.role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getResource() {
        return this.resource;
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

}
