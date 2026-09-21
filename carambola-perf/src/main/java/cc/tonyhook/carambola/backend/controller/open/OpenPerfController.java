package cc.tonyhook.carambola.backend.controller.open;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.entity.perf.EventDelivery;
import cc.tonyhook.carambola.backend.service.perf.ClientChannelService;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.EventDeliveryService;
import cc.tonyhook.carambola.backend.service.perf.EventDuplicateFilterService;
import cc.tonyhook.carambola.backend.service.perf.EventIdValidationService;
import cc.tonyhook.carambola.backend.service.perf.EventRoutingService;
import cc.tonyhook.carambola.backend.service.perf.EventService;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfProcessors;
import cc.tonyhook.carambola.backend.service.perf.media.MediaProcessor;
import cc.tonyhook.carambola.backend.service.perf.track.TrackProcessor;

@RequestMapping("/api/open")
@RestController
public class OpenPerfController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenPerfController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EventService eventService;
    private final EventDuplicateFilterService eventDuplicateFilterService;
    private final EventIdValidationService eventIdValidationService;
    private final EventRoutingService eventRoutingService;
    private final EventDeliveryService eventDeliveryService;
    private final PerfDebugPrintService debugPrintService;

    private final ClientChannelService clientChannelService;

    private final PerfProcessors perfProcessors;

    public OpenPerfController(
            EventService eventService,
            EventDuplicateFilterService eventDuplicateFilterService,
            EventIdValidationService eventIdValidationService,
            EventRoutingService eventRoutingService,
            EventDeliveryService eventDeliveryService,
            PerfDebugPrintService debugPrintService,
            ClientChannelService clientChannelService,
            PerfProcessors perfProcessors
    ) {
        this.eventService = eventService;
        this.eventDuplicateFilterService = eventDuplicateFilterService;
        this.eventIdValidationService = eventIdValidationService;
        this.eventRoutingService = eventRoutingService;
        this.eventDeliveryService = eventDeliveryService;
        this.debugPrintService = debugPrintService;
        this.clientChannelService = clientChannelService;
        this.perfProcessors = perfProcessors;
    }

    @GetMapping(value = "/event", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> event(@RequestParam Map<String, String> queries) {
        try {
            String queriesJson = OBJECT_MAPPER.writeValueAsString(queries);
            debugPrintService.println("   eventA:" + queriesJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (!queries.containsKey("davidia_media")) {
            return ResponseEntity.notFound().build();
        }

        String media = queries.get("davidia_media");
        MediaProcessor mediaProcessor = perfProcessors.media(media);
        if (mediaProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        Event e = mediaProcessor.event(queries);
        if (e == null) {
            return ResponseEntity.notFound().build();
        }

        ClientChannel cc = clientChannelService.getActiveClientChannelByCode(e.getMedia(), e.getMediaCode());
        if (cc == null) {
            return ResponseEntity.notFound().build();
        }
        e.setClientChannel(cc);
        Map<String, String> cleanQueries = mediaProcessor.cleanQueries(e);
        boolean duplicate = eventDuplicateFilterService.markIfDuplicate(e, cleanQueries);
        boolean invalidId = Boolean.TRUE.equals(cc.getFilterInvalidId()) && !eventIdValidationService.hasValidId(cleanQueries);
        e.setDuplicate(duplicate);
        e.setDeviceId(eventIdValidationService.resolveDeviceId(cleanQueries));
        e.setCost(resolveCost(cc, e.getEvent(), duplicate));
        if (duplicate || invalidId) {
            e.setForwarded(false);
            e.setForwardSucceeded(null);
            eventService.dropCallbackUrl(e);
        }
        e = eventService.addEvent(e);
        if (duplicate) {
            return ResponseEntity.ok().build();
        }
        if (invalidId) {
            LOGGER.info(
                "Event blocked by invalid id filter: event={}, channel={}",
                e.getId(), cc.getId());
            return ResponseEntity.ok().build();
        }

        List<ClientChannelRoute> routes = eventRoutingService.resolve(e);
        boolean forwarded = forwardToRoutes(e, routes);
        return forwarded ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private BigDecimal resolveCost(ClientChannel clientChannel, String event, boolean duplicate) {
        if (!EventCodes.CLICK.equals(event) || duplicate) {
            return null;
        }
        if (clientChannel.getCpc() == null) {
            return BigDecimal.ZERO;
        }

        return clientChannel.getCpc();
    }

    private String resolveCallbackDeviceId(Event event) {
        if (event.getDeviceId() != null) {
            return event.getDeviceId();
        }

        return eventIdValidationService.resolveDeviceId(event.getQueries());
    }

    private boolean forwardToRoutes(Event event, List<ClientChannelRoute> routes) {
        boolean anyForwarded = false;
        boolean allSucceeded = true;

        for (ClientChannelRoute route : routes) {
            EventDelivery delivery = eventDeliveryService.create(event, route);
            if (delivery == null) {
                LOGGER.warn(
                    "Event delivery missing for route forwarding: event={}, channel={}, route={}",
                    event.getId(), event.getClientChannel().getId(), route.getId());
                allSucceeded = false;
                continue;
            }
            TrackProcessor trackProcessor = perfProcessors.track(route.getTrackName());
            DeliveryResult result = trackProcessor == null
                ? DeliveryResult.UNSUPPORTED
                : trackProcessor.event(event, route, delivery.getToken());
            if (result == DeliveryResult.UNSUPPORTED) {
                // 没有这个监测方,或它不接收这个事件:路由配错了,请求没有发出
                LOGGER.warn(
                    "Route not deliverable: event={}, route={}, track={}, eventCode={}",
                    event.getId(), route.getId(), route.getTrackName(), event.getEvent());
                allSucceeded = false;
                eventDeliveryService.complete(delivery, false, null);
                continue;
            }

            boolean succeeded = result == DeliveryResult.SUCCEEDED;
            anyForwarded = true;
            allSucceeded = allSucceeded && succeeded;
            eventDeliveryService.complete(delivery, true, succeeded);
        }

        event.setForwarded(anyForwarded);
        event.setForwardSucceeded(anyForwarded ? allSucceeded : null);
        if (!anyForwarded) {
            eventService.dropCallbackUrl(event);
        }
        eventService.updateEvent(event);
        return anyForwarded;
    }

    @GetMapping(value = "/callback", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> callback(@RequestParam Map<String, String> queries) {
        try {
            String queriesJson = OBJECT_MAPPER.writeValueAsString(queries);
            debugPrintService.println("callbackA:" + queriesJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (!queries.containsKey("davidia_track")) {
            return ResponseEntity.notFound().build();
        }

        String track = queries.get("davidia_track");
        TrackProcessor trackProcessor = perfProcessors.track(track);
        if (trackProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        Event c = trackProcessor.callback(queries);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }

        EventDelivery delivery = eventDeliveryService.getByToken(queries.get("davidia_delivery"));
        if (delivery == null) {
            return ResponseEntity.notFound().build();
        }

        // 回传所归因的入口事件:媒体打进来、由我们转发出去并换得这个 delivery 的那个事件,
        // 通常是点击,也可能是展示或任何其它转发出去的事件
        Event e = delivery.getEvent();
        if (e == null) {
            return ResponseEntity.notFound().build();
        }
        eventService.loadCallbackUrl(e);

        ClientChannel cc = e.getClientChannel();
        if (cc == null) {
            return ResponseEntity.notFound().build();
        }
        c.setClientChannel(cc);
        c.setDuplicate(false);
        c.setDeviceId(resolveCallbackDeviceId(e));
        c.setEventDelivery(delivery);
        c = eventService.addCallback(c);

        String media = cc.getMediaName();
        MediaProcessor mediaProcessor = perfProcessors.media(media);
        if (mediaProcessor == null) {
            c.setForwarded(false);
            c.setForwardSucceeded(null);
            eventService.updateEvent(c);
            return ResponseEntity.notFound().build();
        }

        // 媒体不接收这类转化时请求没有发出,记为未转发,不算转发失败
        DeliveryResult result = mediaProcessor.callback(c, e);
        c.setForwarded(result != DeliveryResult.UNSUPPORTED);
        c.setForwardSucceeded(result == DeliveryResult.UNSUPPORTED ? null : result == DeliveryResult.SUCCEEDED);
        eventService.updateEvent(c);

        return ResponseEntity.ok().body("{\"code\":0}");
    }

}
