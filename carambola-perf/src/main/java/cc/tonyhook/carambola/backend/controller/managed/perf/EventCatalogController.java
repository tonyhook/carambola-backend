package cc.tonyhook.carambola.backend.controller.managed.perf;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.perf.EventCatalog;
import cc.tonyhook.carambola.backend.service.perf.EventCatalogService;

@RestController
public class EventCatalogController {

    private final EventCatalogService eventCatalogService;

    public EventCatalogController(EventCatalogService eventCatalogService) {
        this.eventCatalogService = eventCatalogService;
    }

    @GetMapping(value = "/api/managed/perf/event-catalog/media-events", produces = "application/json; charset=UTF-8")
    public ResponseEntity<EventCatalog.EventOptions> getMediaEvents() {
        return ResponseEntity.ok(eventCatalogService.getMediaEvents());
    }

}
