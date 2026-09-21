package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.TenantRepository;
import cc.tonyhook.carambola.backend.dao.perf.ClientProjectRepository;
import cc.tonyhook.carambola.backend.dao.security.RoleRepository;
import cc.tonyhook.carambola.backend.dao.security.UserRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.entity.perf.Tenant;
import cc.tonyhook.carambola.backend.entity.perf.TenantUser;
import cc.tonyhook.carambola.backend.entity.security.Role;
import cc.tonyhook.carambola.backend.entity.security.User;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class TenantService {

    private final AuthenticationService authenticationService;

    private final ClientProjectRepository clientProjectRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TenantService(
            AuthenticationService authenticationService,
            ClientProjectRepository clientProjectRepository,
            RoleRepository roleRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository
    ) {
        this.authenticationService = authenticationService;
        this.clientProjectRepository = clientProjectRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    public List<Tenant> queryTenantList(Authentication authentication, Query query) {
        List<Tenant> tenantList = getTenantList(authentication);

        tenantList.removeIf(tenant -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += tenant.getName().toLowerCase();
                    }
                    for (String fragment : query.searchValue.split(" ")) {
                        if (value.contains(fragment.toLowerCase())) {
                            return false;
                        }
                    }
                }
                return true;
            }

            return false;
        });

        return tenantList;
    }

    public List<Tenant> getTenantList(Authentication authentication) {
        List<Tenant> tenantList = tenantRepository.findAllByOrderByUpdateTimeDesc();

        List<Tenant> qualifiedTenantList = new ArrayList<Tenant>();
        String username = authenticationService.getUsername(authentication);

        for (Tenant tenant : tenantList) {
            if (authenticationService.hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
                qualifiedTenantList.add(tenant);
            } else if (tenant != null && tenant.getUser() != null) {
                Boolean qualified = false;
                for (TenantUser user : tenant.getUser()) {
                    if (user.getUsername().equals(username)) {
                        qualified = true;
                        break;
                    }
                }

                if (qualified) {
                    qualifiedTenantList.add(tenant);
                }
            }
        }

        return qualifiedTenantList;
    }

    public Tenant getTenant(Authentication authentication, Integer id) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);

        if (authenticationService.hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
            return tenant;
        }

        if (tenant != null && tenant.getUser() != null) {
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            Iterator<TenantUser> iterator = tenant.getUser().iterator();
            while (iterator.hasNext()) {
                TenantUser user = iterator.next();
                if (!user.getUsername().equals(username)) {
                    iterator.remove();
                }
            }

            if (tenant.getUser().isEmpty()) {
                tenant = null;
            }
        }

        return tenant;
    }

    public Tenant addTenant(Authentication authentication, Tenant newTenant) {
        String username = authenticationService.getUsername(authentication);

        if (newTenant != null && username == null) {
            syncSystemUsers(newTenant);

            newTenant.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newTenant.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Tenant updatedTenant = tenantRepository.save(newTenant);

            return updatedTenant;
        } else {
            return null;
        }
    }

    public Tenant updateTenant(Authentication authentication, Tenant targetTenant, Tenant newTenant) {
        String username = authenticationService.getUsername(authentication);

        if (targetTenant != null && newTenant != null
                && username == null) {
            entityManager.detach(targetTenant);

            syncSystemUsers(newTenant);

            newTenant.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Tenant updatedTenant = tenantRepository.save(newTenant);

            cleanupSystemUsers(targetTenant.getUser());

            return updatedTenant;
        } else {
            return null;
        }
    }

    @Transactional
    public Tenant removeTenant(Authentication authentication, Tenant targetTenant) {
        String username = authenticationService.getUsername(authentication);

        if (targetTenant != null && username == null) {
            tenantRepository.delete(targetTenant);
            cleanupSystemUsers(targetTenant.getUser());

            return targetTenant;
        } else {
            return null;
        }
    }

    private void syncSystemUsers(Tenant tenant) {
        if (tenant.getUser() == null) {
            return;
        }

        for (TenantUser user: tenant.getUser()) {
            user.setTenant(tenant);

            String roleName = getRoleName(user.getRole());
            if (roleName == null) {
                continue;
            }
            final String finalRoleName = roleName;

            Boolean shouldSave = false;

            User systemUser = userRepository.findByUsername(user.getUsername());
            if (systemUser == null) {
                shouldSave = true;

                systemUser = new User();
                systemUser.setUsername(user.getUsername());
                systemUser.setPassword(BCrypt.hashpw(user.getUsername(), BCrypt.gensalt()));
                systemUser.setRoles(Collections.singleton(roleRepository.findByName(roleName)));
                systemUser.setEnabled(true);
            } else {
                if (!systemUser.getRoles().stream().map(existingRole -> existingRole.getName()).anyMatch(name -> name.equals(finalRoleName))) {
                    shouldSave = true;

                    systemUser.getRoles().add(roleRepository.findByName(roleName));
                }
            }

            if (shouldSave) {
                userRepository.save(systemUser);
            }
        }
    }

    private void cleanupSystemUsers(List<TenantUser> originalUsers) {
        if (originalUsers == null) {
            return;
        }

        List<Tenant> tenantList = tenantRepository.findAll();
        for (TenantUser originalUser : originalUsers) {
            Boolean shouldNotDelete = false;
            for (Tenant tenant : tenantList) {
                if (tenant.getUser().stream().anyMatch(user -> user.getUsername().equals(originalUser.getUsername()) && user.getRole().equals(originalUser.getRole()))) {
                    shouldNotDelete = true;
                }
            }

            if (!shouldNotDelete) {
                String roleName = getRoleName(originalUser.getRole());
                if (roleName == null) {
                    continue;
                }

                User systemUser = userRepository.findByUsername(originalUser.getUsername());
                if (systemUser != null) {
                    Iterator<Role> iterator = systemUser.getRoles().iterator();
                    while (iterator.hasNext()) {
                        Role role = iterator.next();
                        if (role.getName().equals(roleName)) {
                            iterator.remove();
                        }
                    }

                    userRepository.save(systemUser);
                }
            }
        }
    }

    private String getRoleName(Integer role) {
        return switch (role) {
            case TenantUser.ROLE_TENANT_MANAGER -> "效果租户管理员";
            case TenantUser.ROLE_CLIENT_MANAGER -> "效果客户管理员";
            case TenantUser.ROLE_CLIENT_PROJECT_MANAGER -> "效果产品管理员";
            default -> null;
        };
    }

    public Tenant syncResourceUsers(Authentication authentication, Integer tenantId, List<TenantUser> users) {
        Tenant targetTenant = tenantRepository.findById(tenantId).orElse(null);
        if (targetTenant == null || users == null) {
            return null;
        }

        for (TenantUser user : users) {
            if (!canSyncResourceUser(authentication, targetTenant, user)) {
                return null;
            }
        }

        entityManager.detach(targetTenant);

        if (targetTenant.getUser() == null) {
            targetTenant.setUser(new ArrayList<TenantUser>());
        }

        for (TenantUser user : users) {
            targetTenant.getUser().removeIf(existing ->
                existing.getRole().equals(user.getRole())
                && (existing.getResource() == null && user.getResource() == null
                    || existing.getResource() != null && existing.getResource().equals(user.getResource()))
            );
        }

        for (TenantUser user : users) {
            if (StringUtils.isEmpty(user.getUsername())) {
                continue;
            }

            user.setId(null);
            user.setTenant(targetTenant);
            targetTenant.getUser().add(user);
        }

        syncSystemUsers(targetTenant);
        targetTenant.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        return tenantRepository.save(targetTenant);
    }

    public List<TenantUser> getResourceUsers(Authentication authentication, Integer tenantId, Integer role, Integer resource) {
        Tenant targetTenant = tenantRepository.findById(tenantId).orElse(null);
        TenantUser checkUser = new TenantUser();
        checkUser.setRole(role);
        checkUser.setResource(resource);

        if (targetTenant == null || !canSyncResourceUser(authentication, targetTenant, checkUser)) {
            return null;
        }

        List<TenantUser> users = new ArrayList<TenantUser>();
        if (targetTenant.getUser() == null) {
            return users;
        }

        for (TenantUser user : targetTenant.getUser()) {
            if (user.getRole().equals(role)
                && user.getResource() != null
                && user.getResource().equals(resource)) {
                users.add(user);
            }
        }

        return users;
    }

    private Boolean canSyncResourceUser(Authentication authentication, Tenant tenant, TenantUser user) {
        if (user == null || user.getRole() == null || user.getResource() == null) {
            return false;
        }

        if (authenticationService.hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
            return true;
        }

        if (user.getRole().equals(TenantUser.ROLE_CLIENT_PROJECT_MANAGER)) {
            ClientProject clientProject = clientProjectRepository.findById(user.getResource()).orElse(null);
            return clientProject != null
                && clientProject.getClient() != null
                && clientProject.getClient().getTenant() != null
                && clientProject.getClient().getTenant().getId().equals(tenant.getId())
                && authenticationService.hasAccess(authentication, clientProject.getClient());
        }

        return false;
    }

}
