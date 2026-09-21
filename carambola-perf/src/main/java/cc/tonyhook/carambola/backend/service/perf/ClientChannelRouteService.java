package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientChannelRouteRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.TenantUser;

@Service
public class ClientChannelRouteService {

    private final AuthenticationService authenticationService;
    private final ClientChannelService clientChannelService;
    private final ClientChannelRouteRepository clientChannelRouteRepository;

    public ClientChannelRouteService(
            AuthenticationService authenticationService,
            ClientChannelService clientChannelService,
            ClientChannelRouteRepository clientChannelRouteRepository
    ) {
        this.authenticationService = authenticationService;
        this.clientChannelService = clientChannelService;
        this.clientChannelRouteRepository = clientChannelRouteRepository;
    }

    public List<ClientChannelRoute> getRoutes(Authentication authentication, Integer clientChannelId) {
        ClientChannel channel = clientChannelService.getClientChannel(authentication, clientChannelId);
        if (channel == null) {
            return null;
        }
        return clientChannelRouteRepository.findActiveRoutesByChannel(clientChannelId);
    }

    public List<ClientChannelRoute> getRoutes(Authentication authentication) {
        List<Integer> clientChannelIds = clientChannelService.getClientChannelList(authentication).stream()
            .filter(channel -> !Boolean.TRUE.equals(channel.getDeleted()))
            .map(ClientChannel::getId)
            .toList();
        if (clientChannelIds.isEmpty()) {
            return List.of();
        }
        return clientChannelRouteRepository.findActiveRoutesByChannels(clientChannelIds);
    }

    public ClientChannelRoute addRoute(
            Authentication authentication,
            Integer clientChannelId,
            ClientChannelRoute newRoute
    ) {
        ClientChannel channel = clientChannelService.getClientChannel(authentication, clientChannelId);
        if (!canManage(authentication, channel) || !isValid(newRoute)) {
            return null;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        newRoute.setId(null);
        newRoute.setClientChannel(channel);
        newRoute.setDeleted(false);
        newRoute.setCreateTime(now);
        newRoute.setUpdateTime(now);
        normalize(newRoute);
        if (hasDuplicateActiveRoute(clientChannelId, null, newRoute)) {
            return null;
        }
        return clientChannelRouteRepository.save(newRoute);
    }

    public ClientChannelRoute updateRoute(
            Authentication authentication,
            Integer clientChannelId,
            Integer routeId,
            ClientChannelRoute newRoute
    ) {
        ClientChannel channel = clientChannelService.getClientChannel(authentication, clientChannelId);
        ClientChannelRoute target = clientChannelRouteRepository.findById(routeId).orElse(null);
        if (!canManage(authentication, channel)
                || target == null
                || target.getClientChannel() == null
                || !clientChannelId.equals(target.getClientChannel().getId())
                || !isValid(newRoute)) {
            return null;
        }

        target.setEvent(newRoute.getEvent());
        target.setTrackName(newRoute.getTrackName());
        target.setTrackCode(newRoute.getTrackCode());
        target.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        normalize(target);
        if (hasDuplicateActiveRoute(clientChannelId, target.getId(), target)) {
            return null;
        }
        return clientChannelRouteRepository.save(target);
    }

    public ClientChannelRoute removeRoute(
            Authentication authentication,
            Integer clientChannelId,
            Integer routeId
    ) {
        ClientChannel channel = clientChannelService.getClientChannel(authentication, clientChannelId);
        ClientChannelRoute target = clientChannelRouteRepository.findById(routeId).orElse(null);
        if (!canManage(authentication, channel)
                || target == null
                || target.getClientChannel() == null
                || !clientChannelId.equals(target.getClientChannel().getId())) {
            return null;
        }

        target.setDeleted(true);
        target.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return clientChannelRouteRepository.save(target);
    }

    private boolean canManage(Authentication authentication, ClientChannel channel) {
        return channel != null
            && channel.getClient() != null
            && channel.getClient().getTenant() != null
            && authenticationService.hasAccess(
                authentication,
                channel.getClient().getTenant(),
                TenantUser.ROLE_TENANT_MANAGER,
                null);
    }

    private boolean isValid(ClientChannelRoute route) {
        return route != null
            && StringUtils.isNotBlank(route.getEvent())
            && StringUtils.isNotBlank(route.getTrackName());
    }

    private void normalize(ClientChannelRoute route) {
        route.setEvent(route.getEvent().trim());
        route.setTrackName(route.getTrackName().trim());
        route.setTrackCode(StringUtils.trimToNull(route.getTrackCode()));
    }

    private boolean hasDuplicateActiveRoute(
            Integer clientChannelId,
            Integer routeId,
            ClientChannelRoute route
    ) {
        return clientChannelRouteRepository.findActiveRoutes(clientChannelId, route.getEvent()).stream()
            .filter(existing -> routeId == null || !routeId.equals(existing.getId()))
            .anyMatch(existing -> Objects.equals(existing.getTrackName(), route.getTrackName())
                && Objects.equals(existing.getTrackCode(), route.getTrackCode()));
    }

}
