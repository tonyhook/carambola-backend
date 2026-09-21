package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientRepository;
import cc.tonyhook.carambola.backend.dao.perf.ClientProjectRepository;
import cc.tonyhook.carambola.backend.dao.perf.TenantRepository;
import cc.tonyhook.carambola.backend.entity.perf.Client;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.entity.perf.Tenant;
import cc.tonyhook.carambola.backend.entity.perf.TenantDefault;
import cc.tonyhook.carambola.backend.entity.perf.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ClientProjectService {

    private final AuthenticationService authenticationService;
    private final TenantDefaultService tenantDefaultService;

    private final ClientRepository clientRepository;
    private final ClientProjectRepository clientProjectRepository;
    private final TenantRepository tenantRepository;

    public ClientProjectService(
            AuthenticationService authenticationService,
            TenantDefaultService tenantDefaultService,
            ClientRepository clientRepository,
            ClientProjectRepository clientProjectRepository,
            TenantRepository tenantRepository
    ) {
        this.authenticationService = authenticationService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientRepository = clientRepository;
        this.clientProjectRepository = clientProjectRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<ClientProject> queryClientProjectList(Authentication authentication, Query query) {
        List<ClientProject> qualifiedClientProjectList = getClientProjectList(authentication);

        qualifiedClientProjectList.removeIf(clientProject -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += clientProject.getName().toLowerCase();
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

        return qualifiedClientProjectList;
    }

    public List<ClientProject> getClientProjectList(Authentication authentication) {
        List<ClientProject> qualifiedClientProjectList = new ArrayList<ClientProject>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<ClientProject> clientProjectList = clientProjectRepository.findAll();
        Set<Integer> accessibleClientIds = tenantDefault == null ? null : authenticationService.getAccessibleClientIds(authentication, tenantDefault.getTenant());
        Set<Integer> accessibleProjectIds = tenantDefault == null ? null : authenticationService.getAccessibleClientProjectIds(authentication, tenantDefault.getTenant());

        for (ClientProject clientProject : clientProjectList) {
            Client client = clientProject.getClient();
            if (tenantDefault != null) {
                if (client != null && client.getTenant() != null && client.getTenant().getId().equals(tenantDefault.getTenant().getId())
                    && (accessibleClientIds == null
                        || accessibleClientIds.contains(client.getId())
                        || accessibleProjectIds == null
                        || accessibleProjectIds.contains(clientProject.getId()))) {
                    qualifiedClientProjectList.add(clientProject);
                }
            } else if (authenticationService.hasAccess(authentication, clientProject)) {
                qualifiedClientProjectList.add(clientProject);
            }
        }

        return qualifiedClientProjectList;
    }

    public ClientProject getClientProject(Authentication authentication, Integer id) {
        ClientProject clientProject = clientProjectRepository.findById(id).orElse(null);

        if (clientProject != null && authenticationService.hasAccess(authentication, clientProject)) {
            return clientProject;
        } else {
            return null;
        }
    }

    public ClientProject addClientProject(Authentication authentication, ClientProject newClientProject) {
        if (newClientProject != null && newClientProject.getClient() != null) {
            Client client = clientRepository.findById(newClientProject.getClient().getId()).orElse(null);

            if (client != null
                && (authenticationService.hasAccess(authentication, client)
                    || hasClientProjectAccess(authentication, client))) {
                newClientProject.setClient(client);
                newClientProject.setCreateTime(new Timestamp(System.currentTimeMillis()));
                newClientProject.setUpdateTime(new Timestamp(System.currentTimeMillis()));
                ClientProject updatedClientProject = clientProjectRepository.save(newClientProject);
                ensureCreatorProjectAccess(authentication, updatedClientProject);

                return updatedClientProject;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public ClientProject updateClientProject(Authentication authentication, ClientProject targetClientProject, ClientProject newClientProject) {
        if (targetClientProject != null
            && newClientProject != null
            && targetClientProject.getClient() != null
            && newClientProject.getClient() != null) {
            Client client = clientRepository.findById(newClientProject.getClient().getId()).orElse(null);

            if (client != null
                && authenticationService.hasAccess(authentication, targetClientProject)
                && (authenticationService.hasAccess(authentication, client)
                    || targetClientProject.getClient().getId().equals(client.getId()))) {
                newClientProject.setClient(client);
                newClientProject.setUpdateTime(new Timestamp(System.currentTimeMillis()));
                ClientProject updatedClientProject = clientProjectRepository.save(newClientProject);

                return updatedClientProject;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Transactional
    public ClientProject removeClientProject(Authentication authentication, ClientProject targetClientProject) {
        if (targetClientProject != null
            && targetClientProject.getClient() != null
            && authenticationService.hasAccess(authentication, targetClientProject)) {
            targetClientProject.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            targetClientProject.setDeleted(true);

            return targetClientProject;
        } else {
            return null;
        }
    }

    private Boolean hasClientProjectAccess(Authentication authentication, Client client) {
        if (client == null || client.getTenant() == null || client.getTenant().getUser() == null) {
            return false;
        }

        String username = authenticationService.getUsername(authentication);
        if (username == null) {
            return true;
        }

        for (TenantUser user : client.getTenant().getUser()) {
            if (!user.getUsername().equals(username)
                || !user.getRole().equals(TenantUser.ROLE_CLIENT_PROJECT_MANAGER)
                || user.getResource() == null) {
                continue;
            }

            ClientProject clientProject = clientProjectRepository.findById(user.getResource()).orElse(null);
            if (clientProject != null
                && clientProject.getClient() != null
                && clientProject.getClient().getId().equals(client.getId())) {
                return true;
            }
        }

        return false;
    }

    private void ensureCreatorProjectAccess(Authentication authentication, ClientProject clientProject) {
        String username = authenticationService.getUsername(authentication);
        if (username == null
            || clientProject == null
            || clientProject.getId() == null
            || clientProject.getClient() == null
            || clientProject.getClient().getTenant() == null
            || authenticationService.hasAccess(authentication, clientProject)) {
            return;
        }

        Tenant tenant = tenantRepository.findById(clientProject.getClient().getTenant().getId()).orElse(null);
        if (tenant == null) {
            return;
        }

        if (tenant.getUser() == null) {
            tenant.setUser(new ArrayList<TenantUser>());
        }

        Boolean exists = false;
        for (TenantUser user : tenant.getUser()) {
            if (user.getUsername().equals(username)
                && user.getRole().equals(TenantUser.ROLE_CLIENT_PROJECT_MANAGER)
                && clientProject.getId().equals(user.getResource())) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            TenantUser user = new TenantUser();
            user.setTenant(tenant);
            user.setUsername(username);
            user.setRole(TenantUser.ROLE_CLIENT_PROJECT_MANAGER);
            user.setResource(clientProject.getId());
            tenant.getUser().add(user);
            tenantRepository.save(tenant);
        }
    }

}
