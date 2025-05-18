package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ClientRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ClientService {

    private final AuthenticationService authenticationService;
    private final ClientMediaService clientMediaService;
    private final TenantDefaultService tenantDefaultService;

    private final ClientRepository clientRepository;

    public ClientService(
            AuthenticationService authenticationService,
            ClientMediaService clientMediaService,
            TenantDefaultService tenantDefaultService,
            ClientRepository clientRepository
    ) {
        this.authenticationService = authenticationService;
        this.clientMediaService = clientMediaService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientRepository = clientRepository;
    }

    public List<Client> queryClientList(Authentication authentication, Query query) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Client> clientList;
        if (tenantDefault == null) {
            clientList = clientRepository.findAll();
        } else {
            clientList = clientRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
        }

        for (Client client : clientList) {
            if (authenticationService.hasAccess(authentication, client)) {
                qualifiedClientList.add(client);
            }
        }

        qualifiedClientList.removeIf(client -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("mode")) {
                    List<String> modeList = query.filter.get(key);
                    Boolean matched = false;
                    for (String mode : modeList) {
                        if (client.getMode().toString().equals(mode.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
            }

            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += client.getName().toLowerCase();
                    }
                    if (key.equals("code")) {
                        if (client.getCode() != null) {
                            value += client.getCode().toLowerCase();
                        }
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
        if (tenantDefault == null) {
            clientList = clientRepository.findAll();
        } else {
            clientList = clientRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
        }

        for (Client client : clientList) {
            if (authenticationService.hasAccess(authentication, client)) {
                qualifiedClientList.add(client);
            }
        }

        return qualifiedClientList;
    }

    public Client getClient(Authentication authentication, Integer id) {
        Client client = clientRepository.findById(id).orElse(null);

        if (client != null && authenticationService.hasAccess(authentication, client)) {
            return client;
        } else {
            return null;
        }
    }

    public Client getClient(Authentication authentication, String code) {
        Client client = clientRepository.findTopByCode(code);

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

            for (ClientMedia clientMedia : targetClient.getClientMedia()) {
                clientMediaService.removeClientMedia(authentication, clientMedia);
            }

            return targetClient;
        } else {
            return null;
        }
    }

}
