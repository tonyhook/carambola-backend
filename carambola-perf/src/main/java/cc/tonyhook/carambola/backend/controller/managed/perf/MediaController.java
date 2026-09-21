package cc.tonyhook.carambola.backend.controller.managed.perf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.perf.Media;
import cc.tonyhook.carambola.backend.service.perf.EventCatalogService;
import cc.tonyhook.carambola.backend.service.perf.MediaService;
import cc.tonyhook.carambola.backend.service.perf.PerfProcessors;
import cc.tonyhook.carambola.backend.service.perf.media.MediaProcessor;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RequestMapping("/api/managed/perf/media")
@RestController
public class MediaController {

    private final MediaService mediaService;
    private final EventCatalogService eventCatalogService;

    private final PerfProcessors perfProcessors;

    public MediaController(
            MediaService mediaService,
            EventCatalogService eventCatalogService,
            PerfProcessors perfProcessors
    ) {
        this.mediaService = mediaService;
        this.eventCatalogService = eventCatalogService;
        this.perfProcessors = perfProcessors;
    }

    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Media>> getMediaList(@RequestParam(required = false) String query, Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Media> mediaList = mediaService.queryMediaList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(mediaList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Media> mediaList = mediaService.getMediaList(authentication);

            return ResponseEntity.ok().body(mediaList);
        }
    }

    @GetMapping(value = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Media> getMedia(@PathVariable Integer id, Authentication authentication) {
        Media media = mediaService.getMedia(authentication, id);

        if (media != null) {
            return ResponseEntity.ok().body(media);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{id}/event-url", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> getEventUrl(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String mediaCode,
            @RequestParam(required = false) String mediaEvent,
            Authentication authentication) {
        Media media = mediaService.getMedia(authentication, id);
        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        String mediaName = getResourceKey(media);
        if (mediaName == null || mediaName.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        MediaProcessor mediaProcessor = perfProcessors.media(mediaName);
        if (mediaProcessor == null) {
            return ResponseEntity.notFound().build();
        }

        String event = mediaEvent == null || mediaEvent.isBlank() ? eventCatalogService.getDefaultMediaEvent() : mediaEvent;

        return ResponseEntity.ok(mediaProcessor.getEventUrl(mediaCode, event));
    }

    @PostMapping(consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Media> addMedia(@RequestBody Media newMedia, Authentication authentication) throws URISyntaxException {
        Media updatedMedia = mediaService.addMedia(authentication, newMedia);

        if (updatedMedia != null) {
            return ResponseEntity
                .created(new URI("/api/managed/perf/media/" + updatedMedia.getId()))
                .body(updatedMedia);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateMedia(@PathVariable Integer id, @RequestBody Media newMedia, Authentication authentication) {
        if (!id.equals(newMedia.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Media targetMedia = mediaService.getMedia(authentication, id);
        if (targetMedia == null) {
            return ResponseEntity.notFound().build();
        }

        Media updatedMedia = mediaService.updateMedia(authentication, targetMedia, newMedia);

        if (updatedMedia != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeMedia(@PathVariable Integer id, Authentication authentication) {
        Media targetMedia = mediaService.getMedia(authentication, id);
        if (targetMedia == null) {
            return ResponseEntity.notFound().build();
        }

        Media deletedMedia = mediaService.removeMedia(authentication, targetMedia);

        if (deletedMedia != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private String getResourceKey(Media media) {
        return media.getCode() == null || media.getCode().isBlank() ? media.getName() : media.getCode();
    }

}
