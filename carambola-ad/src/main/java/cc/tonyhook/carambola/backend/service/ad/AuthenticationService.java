package cc.tonyhook.carambola.backend.service.ad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.Tenant;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;

@Service
public class AuthenticationService {

    public String getUsername(Authentication authentication) {
        Boolean isManagement = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("AD_MANAGEMENT")) {
                isManagement = true;
            }
        }

        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        if (isManagement) {
            username = null;
        }

        return username;
    }

    public Boolean hasAccess(Authentication authentication, Tenant tenant, Integer role, Integer resource) {
        String username = getUsername(authentication);

        if (username == null) {
            return true;
        }

        if (tenant == null || tenant.getUser() == null) {
            return false;
        }

        for (TenantUser user : tenant.getUser()) {
            if (user.getUsername().equals(username)
                && user.getRole().equals(role)
                && (user.getResource() == null && resource == null
                    || user.getResource() != null && user.getResource().equals(resource))) {
                return true;
            }
        }

        return false;
    }

    public Boolean hasAccess(Authentication authentication, Client client) {
        String username = getUsername(authentication);

        if (username == null) {
            return true;
        }

        Tenant tenant = client.getTenant();

        if (tenant == null || tenant.getUser() == null) {
            return false;
        }

        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_DIRECT, client.getId())) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_PROGRAMMATIC, client.getId())) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_OPERATOR, null)) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_OBSERVER, null)) {
            return true;
        }

        return false;
    }

    public Boolean hasAccess(Authentication authentication, Vendor vendor) {
        String username = getUsername(authentication);

        if (username == null) {
            return true;
        }

        Tenant tenant = vendor.getTenant();

        if (tenant == null || tenant.getUser() == null) {
            return false;
        }

        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendor.getId())) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendor.getId())) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_OPERATOR, null)) {
            return true;
        }
        if (hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_OBSERVER, null)) {
            return true;
        }

        return false;
    }

}
