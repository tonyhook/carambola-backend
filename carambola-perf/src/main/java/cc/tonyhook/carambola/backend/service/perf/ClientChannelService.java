package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientChannelRepository;
import cc.tonyhook.carambola.backend.dao.perf.ClientChannelRouteRepository;
import cc.tonyhook.carambola.backend.dao.perf.ClientProjectRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.entity.perf.TenantDefault;
import cc.tonyhook.carambola.backend.entity.perf.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ClientChannelService {

    private final AuthenticationService authenticationService;
    private final TenantDefaultService tenantDefaultService;

    private final ClientChannelRepository clientChannelRepository;
    private final ClientChannelRouteRepository clientChannelRouteRepository;
    private final ClientProjectRepository clientProjectRepository;

    public ClientChannelService(
            AuthenticationService authenticationService,
            TenantDefaultService tenantDefaultService,
            ClientChannelRepository clientChannelRepository,
            ClientChannelRouteRepository clientChannelRouteRepository,
            ClientProjectRepository clientProjectRepository
    ) {
        this.authenticationService = authenticationService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientChannelRepository = clientChannelRepository;
        this.clientChannelRouteRepository = clientChannelRouteRepository;
        this.clientProjectRepository = clientProjectRepository;
    }

    public List<ClientChannel> queryClientChannelList(Authentication authentication, Query query) {
        List<ClientChannel> qualifiedClientChannelList = getClientChannelList(authentication);
        List<Integer> channelIds = qualifiedClientChannelList.stream().map(ClientChannel::getId).toList();
        Map<Integer, List<ClientChannelRoute>> routesByChannel = channelIds.isEmpty()
            ? Map.of()
            : clientChannelRouteRepository.findActiveRoutesByChannels(channelIds).stream()
                .collect(Collectors.groupingBy(route -> route.getClientChannel().getId()));

        qualifiedClientChannelList.removeIf(clientChannel -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if ((key.equals("project") || key.equals("clientProject")) && clientChannel.getClientProject() != null) {
                        value += StringUtils.defaultString(clientChannel.getClientProject().getName()).toLowerCase();
                        if (clientChannel.getClientProject().getClient() != null) {
                            value += StringUtils.defaultString(clientChannel.getClientProject().getClient().getName()).toLowerCase();
                        }
                    }
                    if (key.equals("mediaName")) {
                        value += StringUtils.defaultString(clientChannel.getMediaName()).toLowerCase();
                    }
                    if (key.equals("mediaCode")) {
                        value += StringUtils.defaultString(clientChannel.getMediaCode()).toLowerCase();
                    }
                    if (key.equals("operator")) {
                        value += StringUtils.defaultString(clientChannel.getOperator()).toLowerCase();
                    }
                    if (key.equals("trackName")) {
                        value += routesByChannel.getOrDefault(clientChannel.getId(), List.of()).stream()
                            .map(ClientChannelRoute::getTrackName)
                            .map(StringUtils::defaultString)
                            .collect(Collectors.joining(" "))
                            .toLowerCase();
                    }
                    if (key.equals("trackCode")) {
                        value += routesByChannel.getOrDefault(clientChannel.getId(), List.of()).stream()
                            .map(ClientChannelRoute::getTrackCode)
                            .map(StringUtils::defaultString)
                            .collect(Collectors.joining(" "))
                            .toLowerCase();
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

        return qualifiedClientChannelList;
    }

    public List<ClientChannel> getClientChannelList(Authentication authentication) {
        List<ClientChannel> qualifiedClientChannelList = new ArrayList<ClientChannel>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<ClientChannel> clientChannelList = clientChannelRepository.findAll();
        Set<Integer> accessibleClientIds = tenantDefault == null ? null : authenticationService.getAccessibleClientIds(authentication, tenantDefault.getTenant());
        Set<Integer> accessibleProjectIds = tenantDefault == null ? null : authenticationService.getAccessibleClientProjectIds(authentication, tenantDefault.getTenant());

        for (ClientChannel clientChannel : clientChannelList) {
            ClientProject clientProject = clientChannel.getClientProject();
            if (tenantDefault != null) {
                if (clientChannel.getClient() != null
                    && clientChannel.getClient().getTenant() != null
                    && clientChannel.getClient().getTenant().getId().equals(tenantDefault.getTenant().getId())
                    && (accessibleClientIds == null
                        || accessibleClientIds.contains(clientChannel.getClient().getId())
                        || clientProject != null && (accessibleProjectIds == null || accessibleProjectIds.contains(clientProject.getId())))) {
                    qualifiedClientChannelList.add(clientChannel);
                }
            } else if (clientProject != null && authenticationService.hasAccess(authentication, clientProject)
                    || clientChannel.getClient() != null && authenticationService.hasAccess(authentication, clientChannel.getClient())) {
                qualifiedClientChannelList.add(clientChannel);
            }
        }

        return qualifiedClientChannelList;
    }

    public ClientChannel getClientChannel(Authentication authentication, Integer id) {
        ClientChannel clientChannel = clientChannelRepository.findById(id).orElse(null);

        if (clientChannel != null
            && (clientChannel.getClientProject() != null && authenticationService.hasAccess(authentication, clientChannel.getClientProject())
                || clientChannel.getClient() != null && authenticationService.hasAccess(authentication, clientChannel.getClient()))) {
            return clientChannel;
        } else {
            return null;
        }
    }

    public ClientChannel getClientChannelByCode(String media, String mediaCode) {
        if (media == null || mediaCode == null) {
            return null;
        }

        ClientChannel clientChannel = clientChannelRepository.findFirstByMediaNameAndMediaCode(media, mediaCode);

        return clientChannel;
    }

    public ClientChannel getActiveClientChannelByCode(String media, String mediaCode) {
        if (media == null || mediaCode == null) {
            return null;
        }

        ClientChannel clientChannel = clientChannelRepository.findFirstActiveByMediaNameAndMediaCode(media, mediaCode);

        return clientChannel;
    }

    public ClientChannel addClientChannel(Authentication authentication, ClientChannel newClientChannel) {
        fillClient(newClientChannel, null);
        if (newClientChannel != null
            && newClientChannel.getClient() != null
            && authenticationService.hasAccess(authentication, newClientChannel.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            newClientChannel.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newClientChannel.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            ClientChannel updatedClientChannel = clientChannelRepository.save(newClientChannel);

            return updatedClientChannel;
        } else {
            return null;
        }
    }

    public ClientChannel updateClientChannel(Authentication authentication, ClientChannel targetClientChannel, ClientChannel newClientChannel) {
        fillClient(newClientChannel, targetClientChannel);
        if (targetClientChannel != null
            && newClientChannel != null
            && targetClientChannel.getClient() != null
            && authenticationService.hasAccess(authentication, targetClientChannel.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            newClientChannel.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            ClientChannel updatedClientChannel = clientChannelRepository.save(newClientChannel);

            return updatedClientChannel;
        } else {
            return null;
        }
    }

    private void fillClient(ClientChannel clientChannel, ClientChannel fallback) {
        if (clientChannel == null) {
            return;
        }

        ClientProject clientProject = clientChannel.getClientProject();
        if (clientProject != null && clientProject.getId() != null) {
            clientProject = clientProjectRepository.findById(clientProject.getId()).orElse(clientProject);
            clientChannel.setClientProject(clientProject);
        }
        if (clientProject != null && clientProject.getClient() != null) {
            clientChannel.setClient(clientProject.getClient());
        } else if (clientChannel.getClient() == null && fallback != null) {
            clientChannel.setClient(fallback.getClient());
        }
    }

    @Transactional
    public ClientChannel removeClientChannel(Authentication authentication, ClientChannel targetClientChannel) {
        if (targetClientChannel != null
            && targetClientChannel.getClient() != null
            && authenticationService.hasAccess(authentication, targetClientChannel.getClient().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            targetClientChannel.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            targetClientChannel.setDeleted(true);

            return targetClientChannel;
        } else {
            return null;
        }
    }

}
