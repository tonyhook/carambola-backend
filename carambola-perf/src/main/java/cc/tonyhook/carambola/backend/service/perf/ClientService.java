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
import cc.tonyhook.carambola.backend.entity.perf.Client;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.entity.perf.TenantDefault;
import cc.tonyhook.carambola.backend.entity.perf.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ClientService {

    private final AuthenticationService authenticationService;
    private final ClientProjectService clientProjectService;
    private final TenantDefaultService tenantDefaultService;

    private final ClientRepository clientRepository;
    private final ClientProjectRepository clientProjectRepository;

    public ClientService(
            AuthenticationService authenticationService,
            ClientProjectService clientProjectService,
            TenantDefaultService tenantDefaultService,
            ClientRepository clientRepository,
            ClientProjectRepository clientProjectRepository
    ) {
        this.authenticationService = authenticationService;
        this.clientProjectService = clientProjectService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientRepository = clientRepository;
        this.clientProjectRepository = clientProjectRepository;
    }

    public List<Client> queryClientList(Authentication authentication, Query query) {
        List<Client> qualifiedClientList = getClientList(authentication);

        qualifiedClientList.removeIf(client -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += client.getName().toLowerCase();
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

        return qualifiedClientList;
    }

    public List<Client> getClientList(Authentication authentication) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Client> clientList;
        Set<Integer> accessibleIds;
        Set<Integer> accessibleProjectIds;
        if (tenantDefault == null || authenticationService.getUsername(authentication) == null) {
            clientList = clientRepository.findAll();
            accessibleIds = null;
            accessibleProjectIds = null;
        } else {
            clientList = clientRepository.findByTenantIdOrderByUpdateTimeDesc(tenantDefault.getTenant().getId());
            accessibleIds = authenticationService.getAccessibleClientIds(authentication, tenantDefault.getTenant());
            accessibleProjectIds = authenticationService.getAccessibleClientProjectIds(authentication, tenantDefault.getTenant());
        }

        for (Client client : clientList) {
            if (tenantDefault != null) {
                if (accessibleIds == null
                    || accessibleIds.contains(client.getId())
                    || hasAccessibleProject(client, accessibleProjectIds)) {
                    qualifiedClientList.add(client);
                }
            } else if (authenticationService.hasAccess(authentication, client)) {
                qualifiedClientList.add(client);
            }
        }

        return qualifiedClientList;
    }

    private Boolean hasAccessibleProject(Client client, Set<Integer> accessibleProjectIds) {
        if (client == null || accessibleProjectIds == null || accessibleProjectIds.isEmpty()) {
            return false;
        }

        for (Integer projectId : accessibleProjectIds) {
            ClientProject clientProject = clientProjectRepository.findById(projectId).orElse(null);
            if (clientProject != null
                && clientProject.getClient() != null
                && clientProject.getClient().getId().equals(client.getId())) {
                return true;
            }
        }

        return false;
    }

    public Client getClient(Authentication authentication, Integer id) {
        Client client = clientRepository.findById(id).orElse(null);

        if (client != null && authenticationService.hasAccess(authentication, client)) {
            return client;
        } else {
            return null;
        }
    }

    public Client addClient(Authentication authentication, Client newClient) {
        if (newClient != null
            && authenticationService.hasAccess(authentication, newClient.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            newClient.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newClient.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            Client updatedClient = clientRepository.save(newClient);

            return updatedClient;
        } else {
            return null;
        }
    }

    public Client updateClient(Authentication authentication, Client targetClient, Client newClient) {
        if (targetClient != null
            && newClient != null
            && authenticationService.hasAccess(authentication, targetClient.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            newClient.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            Client updatedClient = clientRepository.save(newClient);

            return updatedClient;
        } else {
            return null;
        }
    }

    @Transactional
    public Client removeClient(Authentication authentication, Client targetClient) {
        if (targetClient != null
            && authenticationService.hasAccess(authentication, targetClient.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            targetClient.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            targetClient.setDeleted(true);

            for (ClientProject clientProject : targetClient.getClientProject()) {
                clientProjectService.removeClientProject(authentication, clientProject);
            }

            return targetClient;
        } else {
            return null;
        }
    }

}
