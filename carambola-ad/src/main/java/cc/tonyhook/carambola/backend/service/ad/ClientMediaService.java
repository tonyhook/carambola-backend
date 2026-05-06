package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ClientMediaRepository;
import cc.tonyhook.carambola.backend.dao.ad.ClientRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ClientMediaService {

    private final AuthenticationService authenticationService;
    private final ClientPortService clientPortService;
    private final TenantDefaultService tenantDefaultService;

    private final ClientRepository clientRepository;
    private final ClientMediaRepository clientMediaRepository;

    public ClientMediaService(
            AuthenticationService authenticationService,
            ClientPortService clientPortService,
            TenantDefaultService tenantDefaultService,
            ClientRepository clientRepository,
            ClientMediaRepository clientMediaRepository
    ) {
        this.authenticationService = authenticationService;
        this.clientPortService = clientPortService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientRepository = clientRepository;
        this.clientMediaRepository = clientMediaRepository;
    }

    public List<ClientMedia> queryClientMediaList(Authentication authentication, Query query) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Client> clientList;
        Set<Integer> accessibleIds;
        if (tenantDefault == null) {
            clientList = clientRepository.findAll();
            accessibleIds = null;
        } else {
            clientList = clientRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
            accessibleIds = authenticationService.getAccessibleClientIds(authentication, tenantDefault.getTenant());
        }

        for (Client client : clientList) {
            if (tenantDefault != null) {
                if (accessibleIds == null || accessibleIds.contains(client.getId())) {
                    qualifiedClientList.add(client);
                }
            } else if (authenticationService.hasAccess(authentication, client)) {
                qualifiedClientList.add(client);
            }
        }
        qualifiedClientList.removeIf(client -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("clientMode")) {
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

            return false;
        });

        List<ClientMedia> clientMediaList = clientMediaRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        clientMediaList.removeIf(clientMedia -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("client")) {
                    List<String> clientIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientId : clientIdList) {
                        if (clientMedia.getClient().getId().toString().equals(clientId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("platform")) {
                    List<String> platformList = query.filter.get(key);
                    Boolean matched = false;
                    for (String platform : platformList) {
                        if (clientMedia.getPlatform().toLowerCase().equals(platform.toLowerCase())) {
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
                        value += clientMedia.getName().toLowerCase();
                    }
                    if (key.equals("apppackage")) {
                        if (clientMedia.getApppackage() != null) {
                            value += clientMedia.getApppackage().toLowerCase();
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

        return clientMediaList;
    }

    public List<ClientMedia> getClientMediaList(Authentication authentication) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Client> clientList;
        Set<Integer> accessibleIds;
        if (tenantDefault == null) {
            clientList = clientRepository.findAll();
            accessibleIds = null;
        } else {
            clientList = clientRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
            accessibleIds = authenticationService.getAccessibleClientIds(authentication, tenantDefault.getTenant());
        }

        for (Client client : clientList) {
            if (tenantDefault != null) {
                if (accessibleIds == null || accessibleIds.contains(client.getId())) {
                    qualifiedClientList.add(client);
                }
            } else if (authenticationService.hasAccess(authentication, client)) {
                qualifiedClientList.add(client);
            }
        }

        List<ClientMedia> clientMediaList = clientMediaRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        return clientMediaList;
    }

    public String getClientMediaListStamp(Authentication authentication) {
        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Object[]> rows;
        if (tenantDefault == null) {
            rows = clientMediaRepository.getStamp();
        } else {
            rows = clientMediaRepository.getStampByTenant(tenantDefault.getTenant().getId());
        }

        Object[] row = rows != null && !rows.isEmpty() ? rows.get(0) : null;
        Object mediaUpdate = row != null && row.length > 0 ? row[0] : null;
        Object mediaCount = row != null && row.length > 1 ? row[1] : null;
        Object clientUpdate = row != null && row.length > 2 ? row[2] : null;

        return String.valueOf(mediaUpdate) + "-" + String.valueOf(mediaCount) + "-" + String.valueOf(clientUpdate);
    }

    public List<ClientMedia> getClientMediaList(Authentication authentication, Client client) {
        List<ClientMedia> qualifiedClientMediaList = new ArrayList<ClientMedia>();

        if (client != null && authenticationService.hasAccess(authentication, client)) {
            List<ClientMedia> clientMediaList = clientMediaRepository.findByClientInOrderByUpdateTimeDesc(Arrays.asList(client));

            for (ClientMedia clientMedia : clientMediaList) {
                if (authenticationService.hasAccess(authentication, clientMedia.getClient())) {
                    qualifiedClientMediaList.add(clientMedia);
                }
            }
        }

        return qualifiedClientMediaList;
    }

    public List<ClientMedia> getClientMediaList(Authentication authentication, List<Client> clientList) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        if (clientList != null) {
            for (Client client : clientList) {
                if (authenticationService.hasAccess(authentication, client)) {
                    qualifiedClientList.add(client);
                }
            }
        }

        List<ClientMedia> clientMediaList = clientMediaRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        return clientMediaList;
    }

    public ClientMedia getClientMedia(Authentication authentication, Integer id) {
        ClientMedia clientMedia = clientMediaRepository.findById(id).orElse(null);

        if (clientMedia != null && authenticationService.hasAccess(authentication, clientMedia.getClient())) {
            return clientMedia;
        } else {
            return null;
        }
    }

    public ClientMedia addClientMedia(Authentication authentication, ClientMedia newClientMedia) {
        if (newClientMedia != null && newClientMedia.getClient() != null) {
            Client client = clientRepository.findById(newClientMedia.getClient().getId()).orElse(null);

            if (client != null
                && authenticationService.hasAccess(authentication, client.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
                newClientMedia.setClient(client);
                newClientMedia.setCreateTime(new Timestamp(System.currentTimeMillis()));
                newClientMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                ClientMedia updatedClientMedia = clientMediaRepository.save(newClientMedia);

                return updatedClientMedia;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public ClientMedia updateClientMedia(Authentication authentication, ClientMedia targetClientMedia, ClientMedia newClientMedia) {
        if (targetClientMedia != null && targetClientMedia.getId() != null && newClientMedia != null && newClientMedia.getClient() != null) {
            ClientMedia clientMedia = clientMediaRepository.findById(targetClientMedia.getId()).orElse(null);
            Client client = clientRepository.findById(newClientMedia.getClient().getId()).orElse(null);

            if (clientMedia != null
                && client != null
                && authenticationService.hasAccess(authentication, clientMedia.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
                newClientMedia.setClient(client);
                newClientMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                ClientMedia updatedClientMedia = clientMediaRepository.save(newClientMedia);

                return updatedClientMedia;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Transactional
    public ClientMedia removeClientMedia(Authentication authentication, ClientMedia targetClientMedia) {
        if (targetClientMedia != null && targetClientMedia.getId() != null) {
            ClientMedia clientMedia = clientMediaRepository.findById(targetClientMedia.getId()).orElse(null);

            if (clientMedia != null
                && authenticationService.hasAccess(authentication, clientMedia.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
                clientMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                clientMedia.setDeleted(true);

                for (ClientPort clientPort : clientMedia.getClientPort()) {
                    clientPortService.removeClientPort(authentication, clientPort);
                }

                return clientMedia;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
