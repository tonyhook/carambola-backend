package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventDeliveryRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventDelivery;

@Service
public class EventDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDeliveryService.class);

    private final EventDeliveryRepository eventDeliveryRepository;

    public EventDeliveryService(EventDeliveryRepository eventDeliveryRepository) {
        this.eventDeliveryRepository = eventDeliveryRepository;
    }

    public EventDelivery getByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return eventDeliveryRepository.findFirstByToken(token);
    }

    public EventDelivery create(Event event, ClientChannelRoute route) {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            EventDelivery delivery = new EventDelivery();
            delivery.setEvent(event);
            delivery.setRoute(route);
            delivery.setToken(UUID.randomUUID().toString());
            delivery.setTrackName(route.getTrackName());
            delivery.setTrackCode(route.getTrackCode());
            delivery.setCreateTime(now);
            delivery.setUpdateTime(now);
            return eventDeliveryRepository.save(delivery);
        } catch (Exception e) {
            LOGGER.error("Cannot create event delivery: event={}, route={}", event.getId(), route.getId(), e);
            return null;
        }
    }

    public void complete(EventDelivery delivery, boolean forwarded, Boolean succeeded) {
        if (delivery == null) {
            return;
        }
        try {
            delivery.setForwarded(forwarded);
            delivery.setSucceeded(succeeded);
            delivery.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            eventDeliveryRepository.save(delivery);
        } catch (Exception e) {
            LOGGER.error("Cannot update event delivery: delivery={}", delivery.getId(), e);
        }
    }

}
