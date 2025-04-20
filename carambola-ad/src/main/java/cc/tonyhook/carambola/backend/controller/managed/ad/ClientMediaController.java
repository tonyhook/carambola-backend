package cc.tonyhook.carambola.backend.controller.managed.ad;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.service.ad.ClientMediaService;
import cc.tonyhook.carambola.backend.service.ad.ClientService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class ClientMediaController {

    private final ClientService clientService;
    private final ClientMediaService clientMediaService;

    public ClientMediaController(ClientService clientService, ClientMediaService clientMediaService) {
        this.clientService = clientService;
        this.clientMediaService = clientMediaService;
    }

    @GetMapping(value = "/api/managed/clientmedia", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientMedia>> getClientMediaList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<ClientMedia> clientMediaList = clientMediaService.queryClientMediaList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(clientMediaList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<ClientMedia> clientMediaList = clientMediaService.getClientMediaList(authentication);

            return ResponseEntity.ok().body(clientMediaList);
        }
    }

    @GetMapping(value = "/api/managed/clientmedia/client/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientMedia>> getClientMediaListByClient(
            @PathVariable Integer id,
            Authentication authentication) {
        Client client = clientService.getClient(authentication, id);
        List<ClientMedia> clientMediaList = clientMediaService.getClientMediaList(authentication, client);

        return ResponseEntity.ok().body(clientMediaList);
    }

    @GetMapping(value = "/api/managed/clientmedia/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ClientMedia> getClientMedia(
            @PathVariable Integer id,
            Authentication authentication) {
        ClientMedia clientMedia = clientMediaService.getClientMedia(authentication, id);

        if (clientMedia != null) {
            return ResponseEntity.ok().body(clientMedia);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/clientmedia", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<ClientMedia> addClientMedia(
            @RequestBody ClientMedia newClientMedia,
            Authentication authentication) throws URISyntaxException {
        ClientMedia updatedClientMedia = clientMediaService.addClientMedia(authentication, newClientMedia);

        if (updatedClientMedia != null) {
            return ResponseEntity
                .created(new URI("/api/managed/clientmedia/" + updatedClientMedia.getId()))
                .body(updatedClientMedia);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/clientmedia/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateClientMedia(
            @PathVariable Integer id,
            @RequestBody ClientMedia newClientMedia,
            Authentication authentication) {
        if (!id.equals(newClientMedia.getId())) {
            return ResponseEntity.badRequest().build();
        }

        ClientMedia targetClientMedia = clientMediaService.getClientMedia(authentication, id);
        if (targetClientMedia == null) {
            return ResponseEntity.notFound().build();
        }

        ClientMedia updatedClientMedia = clientMediaService.updateClientMedia(authentication, targetClientMedia, newClientMedia);

        if (updatedClientMedia != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/clientmedia/{id}")
    public ResponseEntity<?> removeClientMedia(
            @PathVariable Integer id,
            Authentication authentication) {
        ClientMedia targetClientMedia = clientMediaService.getClientMedia(authentication, id);
        if (targetClientMedia == null) {
            return ResponseEntity.notFound().build();
        }

        ClientMedia deletedClientMedia = clientMediaService.removeClientMedia(authentication, targetClientMedia);

        if (deletedClientMedia != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
