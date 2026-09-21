package cc.tonyhook.carambola.backend.controller.managed.perf;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.perf.CountView;
import cc.tonyhook.carambola.backend.entity.perf.EventPageView;
import cc.tonyhook.carambola.backend.service.perf.CountService;
import cc.tonyhook.carambola.backend.service.perf.EventService;

@RequestMapping("/api/managed/perf/count")
@RestController
public class CountController {

    private final CountService countService;
    private final EventService eventService;

    public CountController(
            CountService countService,
            EventService eventService
    ) {
        this.countService = countService;
        this.eventService = eventService;
    }

    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<CountView>> getCountList(
            @RequestParam(defaultValue = "day") String interval,
            @RequestParam(defaultValue = "client") String level,
            @RequestParam(defaultValue = "0") Integer timezoneOffset,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-02-01T00:00:00+08:00") String end,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer clientProjectId,
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String operator,
            Authentication authentication) {
        if (!"hour".equals(interval) && !"day".equals(interval)) {
            return ResponseEntity.badRequest().build();
        }
        if (!"client".equals(level) && !"project".equals(level) && !"channel".equals(level)) {
            return ResponseEntity.badRequest().build();
        }
        if (timezoneOffset < -840 || timezoneOffset > 840) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Timestamp startTimestamp = Timestamp.from(OffsetDateTime.parse(start).toInstant());
            Timestamp endTimestamp = Timestamp.from(OffsetDateTime.parse(end).toInstant());
            List<CountView> countList = countService.queryCountList(
                authentication,
                startTimestamp,
                endTimestamp,
                interval,
                timezoneOffset,
                level,
                clientId,
                clientProjectId,
                media,
                operator);

            return ResponseEntity.ok().body(countList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/events", produces = "application/json; charset=UTF-8")
    public ResponseEntity<EventPageView> getRecentEvents(
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-02-01T00:00:00+08:00") String end,
            @RequestParam String event,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer clientProjectId,
            @RequestParam(required = false) Integer clientChannelId,
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String operator,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication) {
        if (page < 0 || size < 1 || size > 50) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Timestamp startTimestamp = Timestamp.from(OffsetDateTime.parse(start).toInstant());
            Timestamp endTimestamp = Timestamp.from(OffsetDateTime.parse(end).toInstant());
            EventPageView events = eventService.getRecentEvents(
                authentication,
                startTimestamp,
                endTimestamp,
                event,
                clientId,
                clientProjectId,
                clientChannelId,
                media,
                operator,
                page,
                size);

            return ResponseEntity.ok().body(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
