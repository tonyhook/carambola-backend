package cc.tonyhook.carambola.backend.service.ad;

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

import cc.tonyhook.carambola.backend.dao.ad.TenantRepository;
import cc.tonyhook.carambola.backend.dao.security.RoleRepository;
import cc.tonyhook.carambola.backend.dao.security.UserRepository;
import cc.tonyhook.carambola.backend.entity.ad.Tenant;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.entity.security.Role;
import cc.tonyhook.carambola.backend.entity.security.User;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class TenantService {

    private final AuthenticationService authenticationService;

    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TenantService(
            AuthenticationService authenticationService,
            RoleRepository roleRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository
    ) {
        this.authenticationService = authenticationService;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    public List<Tenant> queryTenantList(Authentication authentication, Query query) {
        List<Tenant> tenantList = tenantRepository.findAllByOrderByUpdateTimeDesc();

        List<Tenant> qualifiedTenantList = new ArrayList<Tenant>();

        for (Tenant tenant : tenantList) {
            if (authenticationService.hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
                qualifiedTenantList.add(tenant);
            } else {
                Boolean qualified = false;
                for (TenantUser user : tenant.getUser()) {
                    if (user.getUsername().equals(authenticationService.getUsername(authentication))) {
                        qualified = true;
                        break;
                    }
                }

                if (qualified) {
                    qualifiedTenantList.add(tenant);
                }
            }
        }

        qualifiedTenantList.removeIf(tenant -> {
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

        return qualifiedTenantList;
    }

    public List<Tenant> getTenantList(Authentication authentication) {
        List<Tenant> tenantList = tenantRepository.findAllByOrderByUpdateTimeDesc();

        List<Tenant> qualifiedTenantList = new ArrayList<Tenant>();

        for (Tenant tenant : tenantList) {
            if (authenticationService.hasAccess(authentication, tenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
                qualifiedTenantList.add(tenant);
            } else {
                Boolean qualified = false;
                for (TenantUser user : tenant.getUser()) {
                    if (user.getUsername().equals(((UserDetails) authentication.getPrincipal()).getUsername())) {
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

        if (tenant != null) {
            Iterator<TenantUser> iterator = tenant.getUser().iterator();
            while (iterator.hasNext()) {
                TenantUser user = iterator.next();
                if (!user.getUsername().equals(((UserDetails) authentication.getPrincipal()).getUsername())) {
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
            for (TenantUser user: newTenant.getUser()) {
                user.setTenant(newTenant);

                String roleName = null;
                switch (user.getRole()) {
                    case TenantUser.ROLE_TENANT_MANAGER:
                        roleName = "租户管理员";
                        break;
                    case TenantUser.ROLE_TENANT_OPERATOR:
                        roleName = "租户运营";
                        break;
                    case TenantUser.ROLE_TENANT_OBSERVER:
                        roleName = "租户观察员";
                        break;
                    case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_DIRECT:
                        roleName = "上游观察员-直通";
                        break;
                    case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_PROGRAMMATIC:
                        roleName = "上游观察员-平台";
                        break;
                    case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT:
                        roleName = "下游管理员-直通";
                        break;
                    case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC:
                        roleName = "下游管理员-平台";
                        break;
                    default:
                        roleName = "未知";
                        break;
                }
                if (roleName.equals("未知")) {
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
                    if (!systemUser.getRoles().stream().map(role -> role.getName()).anyMatch(name -> name.equals(finalRoleName))) {
                        shouldSave = true;

                        systemUser.getRoles().add(roleRepository.findByName(roleName));
                    }
                }

                if (shouldSave) {
                    userRepository.save(systemUser);
                }
            }

            newTenant.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newTenant.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Tenant updatedTenant = tenantRepository.save(newTenant);

            return updatedTenant;
        } else {
            return null;
        }
    }

    public Tenant updateTenant(Authentication authentication, Tenant targetTenant, Tenant newTenant) {
        if (targetTenant != null && newTenant != null
                && authenticationService.hasAccess(authentication, targetTenant, TenantUser.ROLE_TENANT_MANAGER, null)) {
            // detach targetTenant to stop JPA from updating it
            entityManager.detach(targetTenant);

            for (TenantUser user : newTenant.getUser()) {
                user.setTenant(newTenant);

                String roleName = null;
                switch (user.getRole()) {
                    case TenantUser.ROLE_TENANT_MANAGER:
                        roleName = "租户管理员";
                        break;
                    case TenantUser.ROLE_TENANT_OPERATOR:
                        roleName = "租户运营";
                        break;
                    case TenantUser.ROLE_TENANT_OBSERVER:
                        roleName = "租户观察员";
                        break;
                    case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_DIRECT:
                        roleName = "上游观察员-直通";
                        break;
                    case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_PROGRAMMATIC:
                        roleName = "上游观察员-平台";
                        break;
                    case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT:
                        roleName = "下游管理员-直通";
                        break;
                    case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC:
                        roleName = "下游管理员-平台";
                        break;
                    default:
                        roleName = "未知";
                        break;
                }
                if (roleName.equals("未知")) {
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
                    if (!systemUser.getRoles().stream().map(role -> role.getName()).anyMatch(name -> name.equals(finalRoleName))) {
                        shouldSave = true;

                        systemUser.getRoles().add(roleRepository.findByName(roleName));
                    }
                }

                if (shouldSave) {
                    userRepository.save(systemUser);
                }
            }

            newTenant.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Tenant updatedTenant = tenantRepository.save(newTenant);

            List<Tenant> tenantList = tenantRepository.findAll();
            for (TenantUser originalUser : targetTenant.getUser()) {
                Boolean shouldNotDelete = false;
                for (Tenant tenant : tenantList) {
                    if (tenant.getUser().stream().anyMatch(user -> user.getUsername().equals(originalUser.getUsername()) && user.getRole().equals(originalUser.getRole()))) {
                        shouldNotDelete = true;
                    }
                }

                if (!shouldNotDelete) {
                    String roleName = null;
                    switch (originalUser.getRole()) {
                        case TenantUser.ROLE_TENANT_MANAGER:
                            roleName = "租户管理员";
                            break;
                        case TenantUser.ROLE_TENANT_OPERATOR:
                            roleName = "租户运营";
                            break;
                        case TenantUser.ROLE_TENANT_OBSERVER:
                            roleName = "租户观察员";
                            break;
                        case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_DIRECT:
                            roleName = "上游观察员-直通";
                            break;
                        case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_PROGRAMMATIC:
                            roleName = "上游观察员-平台";
                            break;
                        case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT:
                            roleName = "下游管理员-直通";
                            break;
                        case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC:
                            roleName = "下游管理员-平台";
                            break;
                        default:
                            roleName = "未知";
                            break;
                    }
                    if (roleName.equals("未知")) {
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

            List<Tenant> tenantList = tenantRepository.findAll();
            for (TenantUser originalUser : targetTenant.getUser()) {
                Boolean shouldNotDelete = false;
                for (Tenant tenant : tenantList) {
                    if (tenant.getUser().stream().anyMatch(user -> user.getUsername().equals(originalUser.getUsername()) && user.getRole().equals(originalUser.getRole()))) {
                        shouldNotDelete = true;
                    }
                }

                if (!shouldNotDelete) {
                    String roleName = null;
                    switch (originalUser.getRole()) {
                        case TenantUser.ROLE_TENANT_MANAGER:
                            roleName = "租户管理员";
                            break;
                        case TenantUser.ROLE_TENANT_OPERATOR:
                            roleName = "租户运营";
                            break;
                        case TenantUser.ROLE_TENANT_OBSERVER:
                            roleName = "租户观察员";
                            break;
                        case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_DIRECT:
                            roleName = "上游观察员-直通";
                            break;
                        case TenantUser.ROLE_TENANT_UPSTREAM_OBSERVER_PROGRAMMATIC:
                            roleName = "上游观察员-平台";
                            break;
                        case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT:
                            roleName = "下游管理员-直通";
                            break;
                        case TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC:
                            roleName = "下游管理员-平台";
                            break;
                        default:
                            roleName = "未知";
                            break;
                    }
                    if (roleName.equals("未知")) {
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

            return targetTenant;
        } else {
            return null;
        }
    }

}
