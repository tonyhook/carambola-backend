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

import cc.tonyhook.carambola.backend.entity.perf.Track;
import cc.tonyhook.carambola.backend.service.perf.TrackService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RequestMapping("/api/managed/perf/track")
@RestController
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Track>> getTrackList(@RequestParam(required = false) String query, Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Track> trackList = trackService.queryTrackList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(trackList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Track> trackList = trackService.getTrackList(authentication);

            return ResponseEntity.ok().body(trackList);
        }
    }

    @GetMapping(value = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Track> getTrack(@PathVariable Integer id, Authentication authentication) {
        Track track = trackService.getTrack(authentication, id);

        if (track != null) {
            return ResponseEntity.ok().body(track);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Track> addTrack(@RequestBody Track newTrack, Authentication authentication) throws URISyntaxException {
        Track updatedTrack = trackService.addTrack(authentication, newTrack);

        if (updatedTrack != null) {
            return ResponseEntity
                .created(new URI("/api/managed/perf/track/" + updatedTrack.getId()))
                .body(updatedTrack);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateTrack(@PathVariable Integer id, @RequestBody Track newTrack, Authentication authentication) {
        if (!id.equals(newTrack.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Track targetTrack = trackService.getTrack(authentication, id);
        if (targetTrack == null) {
            return ResponseEntity.notFound().build();
        }

        Track updatedTrack = trackService.updateTrack(authentication, targetTrack, newTrack);

        if (updatedTrack != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeTrack(@PathVariable Integer id, Authentication authentication) {
        Track targetTrack = trackService.getTrack(authentication, id);
        if (targetTrack == null) {
            return ResponseEntity.notFound().build();
        }

        Track deletedTrack = trackService.removeTrack(authentication, targetTrack);

        if (deletedTrack != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
