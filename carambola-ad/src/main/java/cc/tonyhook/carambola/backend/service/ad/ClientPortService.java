package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ClientMediaRepository;
import cc.tonyhook.carambola.backend.dao.ad.ClientPortRepository;
import cc.tonyhook.carambola.backend.dao.ad.ClientRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ClientPortService {

    private final AuthenticationService authenticationService;
    private final ConnectionService connectionService;
    private final TenantDefaultService tenantDefaultService;

    private final ClientRepository clientRepository;
    private final ClientMediaRepository clientMediaRepository;
    private final ClientPortRepository clientPortRepository;

    public ClientPortService(
            AuthenticationService authenticationService,
            ConnectionService connectionService,
            TenantDefaultService tenantDefaultService,
            ClientRepository clientRepository,
            ClientMediaRepository clientMediaRepository,
            ClientPortRepository clientPortRepository
    ) {
        this.authenticationService = authenticationService;
        this.connectionService = connectionService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientRepository = clientRepository;
        this.clientMediaRepository = clientMediaRepository;
        this.clientPortRepository = clientPortRepository;
    }

    public List<ClientPort> queryClientPortList(Authentication authentication, Query query) {
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

        List<ClientPort> clientPortList = clientPortRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        clientPortList.removeIf(clientPort -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("client")) {
                    List<String> clientIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientId : clientIdList) {
                        if (clientPort.getClient().getId().toString().equals(clientId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("clientMedia")) {
                    List<String> clientMediaIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientMediaId : clientMediaIdList) {
                        if (clientPort.getClientMedia().getId().toString().equals(clientMediaId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("mode")) {
                    List<String> modeList = query.filter.get(key);
                    Boolean matched = false;
                    for (String mode : modeList) {
                        if (clientPort.getMode().toString().equals(mode.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("format")) {
                    List<String> formatList = query.filter.get(key);
                    Boolean matched = false;
                    for (String format : formatList) {
                        if (clientPort.getFormat().toString().equals(format.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("budget")) {
                    List<String> budgetList = query.filter.get(key);
                    Boolean matched = false;
                    for (String budget : budgetList) {
                        if (clientPort.getBudget().toString().equals(budget.toLowerCase())) {
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
                        if (clientPort.getClientMedia().getPlatform().equals(platform)) {
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
                        value += clientPort.getName().toLowerCase();
                    }
                    if (key.equals("tagId")) {
                        if (clientPort.getTagId() != null) {
                            value += clientPort.getTagId().toLowerCase();
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

        return clientPortList;
    }

    public List<ClientPort> getClientPortList(Authentication authentication) {
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

        List<ClientPort> clientPortList = clientPortRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        return clientPortList;
    }

    public List<ClientPort> getClientPortList(Authentication authentication, Client client) {
        List<ClientPort> qualifiedClientPortList = new ArrayList<ClientPort>();

        if (client != null && authenticationService.hasAccess(authentication, client)) {
            List<ClientPort> clientPortList = clientPortRepository.findByClientInOrderByUpdateTimeDesc(Arrays.asList(client));

            for (ClientPort clientPort : clientPortList) {
                if (authenticationService.hasAccess(authentication, clientPort.getClient())) {
                    qualifiedClientPortList.add(clientPort);
                }
            }
        }

        return qualifiedClientPortList;
    }

    public List<ClientPort> getClientPortList(Authentication authentication, List<Client> clientList) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        if (clientList != null) {
            for (Client client : clientList) {
                if (authenticationService.hasAccess(authentication, client)) {
                    qualifiedClientList.add(client);
                }
            }
        }

        List<ClientPort> clientPortList = clientPortRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        return clientPortList;
    }

    public List<ClientPort> getClientPortList(Authentication authentication, ClientMedia clientMedia) {
        List<ClientPort> qualifiedClientPortList = new ArrayList<ClientPort>();

        if (clientMedia != null && authenticationService.hasAccess(authentication, clientMedia.getClient())) {
            List<ClientPort> clientPortList = clientPortRepository.findByClientMediaInOrderByUpdateTimeDesc(Arrays.asList(clientMedia));

            for (ClientPort clientPort : clientPortList) {
                qualifiedClientPortList.add(clientPort);
            }
        }

        return qualifiedClientPortList;
    }

    public ClientPort getClientPort(Authentication authentication, Integer id) {
        ClientPort clientPort = clientPortRepository.findById(id).orElse(null);

        if (clientPort != null && authenticationService.hasAccess(authentication, clientPort.getClient())) {
            return clientPort;
        } else {
            return null;
        }
    }

    public ClientPort addClientPort(Authentication authentication, ClientPort newClientPort) {
        if (newClientPort != null && newClientPort.getClient() != null && newClientPort.getClientMedia() != null) {
            Client client = clientRepository.findById(newClientPort.getClient().getId()).orElse(null);
            ClientMedia clientMedia = clientMediaRepository.findById(newClientPort.getClientMedia().getId()).orElse(null);

            if (client != null
                && clientMedia != null
                && authenticationService.hasAccess(authentication, client.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
                newClientPort.setClient(client);
                newClientPort.setClientMedia(clientMedia);
                newClientPort.setCreateTime(new Timestamp(System.currentTimeMillis()));
                newClientPort.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                ClientPort updatedClientPort = clientPortRepository.save(newClientPort);

                return updatedClientPort;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public ClientPort updateClientPort(Authentication authentication, ClientPort targetClientPort, ClientPort newClientPort) {
        if (targetClientPort != null && targetClientPort.getId() != null
            && newClientPort != null && newClientPort.getClient() != null && newClientPort.getClientMedia() != null) {
            ClientPort clientPort = clientPortRepository.findById(targetClientPort.getId()).orElse(null);
            Client client = clientRepository.findById(newClientPort.getClient().getId()).orElse(null);
            ClientMedia clientMedia = clientMediaRepository.findById(newClientPort.getClientMedia().getId()).orElse(null);

            if (clientPort != null
                && client != null
                && clientMedia != null
                && authenticationService.hasAccess(authentication, clientPort.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
                newClientPort.setClient(client);
                newClientPort.setClientMedia(clientMedia);
                newClientPort.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                ClientPort updatedClientPort = clientPortRepository.save(newClientPort);

                return updatedClientPort;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Transactional
    public ClientPort removeClientPort(Authentication authentication, ClientPort targetClientPort) {
        if (targetClientPort != null && targetClientPort.getId() != null) {
            ClientPort clientPort = clientPortRepository.findById(targetClientPort.getId()).orElse(null);

            if (clientPort != null
                && authenticationService.hasAccess(authentication, clientPort.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
                clientPort.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                clientPort.setDeleted(true);

                for (Connection connection : clientPort.getConnection()) {
                    connectionService.removeConnection(connection.getId());
                }

                return clientPort;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
