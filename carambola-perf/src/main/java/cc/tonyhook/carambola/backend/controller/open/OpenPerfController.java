package cc.tonyhook.carambola.backend.controller.open;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.perf.Callback;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.service.perf.CallbackService;
import cc.tonyhook.carambola.backend.service.perf.ClientChannelService;
import cc.tonyhook.carambola.backend.service.perf.EventService;
import cc.tonyhook.carambola.backend.service.perf.media.MediaProcessor;
import cc.tonyhook.carambola.backend.service.perf.track.TrackProcessor;

@RestController
public class OpenPerfController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EventService eventService;
    private final CallbackService callbackService;

    private final ClientChannelService clientChannelService;

    private final Map<String, MediaProcessor> mediaProcessorMap;
    private final Map<String, TrackProcessor> trackProcessorMap;

    public OpenPerfController(
            EventService eventService,
            CallbackService callbackService,
            ClientChannelService clientChannelService,
            Map<String, MediaProcessor> mediaProcessorMap,
            Map<String, TrackProcessor> trackProcessorMap
    ) {
        this.eventService = eventService;
        this.callbackService = callbackService;
        this.clientChannelService = clientChannelService;
        this.mediaProcessorMap = mediaProcessorMap;
        this.trackProcessorMap = trackProcessorMap;
    }

    @GetMapping(value = "/api/open/event", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> event(@RequestParam Map<String, String> queries) {
        try {
            String queriesJson = OBJECT_MAPPER.writeValueAsString(queries);
            System.out.println("   eventA:" + queriesJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (!queries.containsKey("davidia_media")) {
            return ResponseEntity.notFound().build();
        }

        String media = queries.get("davidia_media");
        MediaProcessor mediaProcessor = mediaProcessorMap.get(getProcessorName("media", media));
        if (mediaProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        Event e = mediaProcessor.event(queries);
        if (e == null) {
            return ResponseEntity.notFound().build();
        }
        e = eventService.addEvent(e);

        ClientChannel cc = clientChannelService.getClientChannelByCode(e.getMedia(), e.getMediaCode());
        if (cc == null) {
            return ResponseEntity.notFound().build();
        }

        String track = cc.getTrackName();
        TrackProcessor trackProcessor = trackProcessorMap.get(getProcessorName("track", track));
        if (trackProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        trackProcessor.event(e, cc);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/open/callback", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> callback(@RequestParam Map<String, String> queries) {
        try {
            String queriesJson = OBJECT_MAPPER.writeValueAsString(queries);
            System.out.println("callbackA:" + queriesJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (!queries.containsKey("davidia_track")) {
            return ResponseEntity.notFound().build();
        }

        String track = queries.get("davidia_track");
        TrackProcessor trackProcessor = trackProcessorMap.get(getProcessorName("track", track));
        if (trackProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        Callback c = trackProcessor.callback(queries);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        c = callbackService.addCallback(c);

        Event e = eventService.getEventBySerialno(c.getSerialno());

        ClientChannel cc = clientChannelService.getClientChannelByCode(e.getMedia(), e.getMediaCode());

        String media = cc.getMediaName();
        MediaProcessor mediaProcessor = mediaProcessorMap.get(getProcessorName("media", media));
        if (mediaProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        mediaProcessor.callback(c, e);

        return ResponseEntity.ok().build();
    }

    private String getProcessorName(String type, String name) {
        return type + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

}
