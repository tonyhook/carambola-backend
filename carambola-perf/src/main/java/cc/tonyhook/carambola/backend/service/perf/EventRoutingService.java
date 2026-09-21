package cc.tonyhook.carambola.backend.service.perf;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientChannelRouteRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.Event;

@Service
public class EventRoutingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRoutingService.class);

    private final ClientChannelRouteRepository clientChannelRouteRepository;

    public EventRoutingService(
            ClientChannelRouteRepository clientChannelRouteRepository
    ) {
        this.clientChannelRouteRepository = clientChannelRouteRepository;
        LOGGER.info("Event routing mode: route-only");
    }

    public List<ClientChannelRoute> resolve(Event event) {
        ClientChannel channel = event.getClientChannel();
        if (channel == null || channel.getId() == null || event.getEvent() == null) {
            return List.of();
        }

        List<ClientChannelRoute> routes;
        try {
            routes = clientChannelRouteRepository.findActiveRoutes(channel.getId(), event.getEvent());
        } catch (Exception e) {
            LOGGER.error(
                "Cannot resolve event routes; event will not be forwarded: event={}, channel={}, eventCode={}",
                event.getId(), channel.getId(), event.getEvent(), e);
            return List.of();
        }

        if (routes.isEmpty()) {
            LOGGER.warn(
                "No active event route: event={}, channel={}, media={}/{}, eventCode={}",
                event.getId(), channel.getId(), channel.getMediaName(), channel.getMediaCode(), event.getEvent());
        }
        return routes;
    }

}
