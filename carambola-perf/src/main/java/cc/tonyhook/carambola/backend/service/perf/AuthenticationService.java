package cc.tonyhook.carambola.backend.service.perf;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.entity.perf.Client;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.entity.perf.Tenant;
import cc.tonyhook.carambola.backend.entity.perf.TenantUser;

@Service("perfAuthenticationService")
public class AuthenticationService {

    public String getUsername(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        if (isManagement(authentication)) {
            username = null;
        }

        return username;
    }

    public Boolean isManagement(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("PERF_MANAGEMENT")) {
                return true;
            }
        }

        return false;
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

    public Set<Integer> getAccessibleClientIds(Authentication authentication, Tenant tenant) {
        String username = getUsername(authentication);

        if (username == null) {
            return null;
        }

        if (tenant == null || tenant.getUser() == null) {
            return Collections.emptySet();
        }

        Set<Integer> ids = new HashSet<Integer>();
        Boolean fullAccess = false;

        for (TenantUser user : tenant.getUser()) {
            if (!user.getUsername().equals(username)) {
                continue;
            }

            Integer role = user.getRole();
            if (role.equals(TenantUser.ROLE_TENANT_MANAGER)) {
                fullAccess = true;
            } else if (role.equals(TenantUser.ROLE_CLIENT_MANAGER)) {
                if (user.getResource() != null) {
                    ids.add(user.getResource());
                }
            }
        }

        if (fullAccess) {
            return null;
        }

        return ids;
    }

    public Set<Integer> getAccessibleClientProjectIds(Authentication authentication, Tenant tenant) {
        String username = getUsername(authentication);

        if (username == null) {
            return null;
        }

        if (tenant == null || tenant.getUser() == null) {
            return Collections.emptySet();
        }

        Set<Integer> ids = new HashSet<Integer>();
        Boolean fullAccess = false;

        for (TenantUser user : tenant.getUser()) {
            if (!user.getUsername().equals(username)) {
                continue;
            }

            Integer role = user.getRole();
            if (role.equals(TenantUser.ROLE_TENANT_MANAGER)) {
                fullAccess = true;
            } else if (role.equals(TenantUser.ROLE_CLIENT_PROJECT_MANAGER)) {
                if (user.getResource() != null) {
                    ids.add(user.getResource());
                }
            }
        }

        if (fullAccess) {
            return null;
        }

        return ids;
    }

    public Boolean hasAccess(Authentication authentication, Client client) {
        if (client == null) {
            return false;
        }

        Tenant tenant = client.getTenant();

        return hasAccess(authentication, tenant, TenantUser.ROLE_CLIENT_MANAGER, client.getId())
            || hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null);
    }

    public Boolean hasAccess(Authentication authentication, ClientProject clientProject) {
        if (clientProject == null || clientProject.getClient() == null) {
            return false;
        }

        Tenant tenant = clientProject.getClient().getTenant();

        return hasAccess(authentication, tenant, TenantUser.ROLE_CLIENT_PROJECT_MANAGER, clientProject.getId())
            || hasAccess(authentication, clientProject.getClient());
    }

}
