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
import org.springframework.web.context.request.WebRequest;

import tools.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.service.ad.ClientService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping(value = "/api/managed/client", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Client>> getClientList(
            @RequestParam(required = false) String query,
            Authentication authentication,
            WebRequest webRequest) {
        String etag = "\"" + clientService.getClientListStamp(authentication)
            + "-" + (authentication != null ? authentication.getName() : "")
            + "-" + (query != null ? Integer.toHexString(query.hashCode()) : "")
            + "\"";

        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).header("Cache-Control", "private, max-age=0, must-revalidate").eTag(etag).build();
        }

        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Client> clientList = clientService.queryClientList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().header("Cache-Control", "private, max-age=0, must-revalidate").eTag(etag).body(clientList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Client> clientList = clientService.getClientList(authentication);

            return ResponseEntity.ok().header("Cache-Control", "private, max-age=0, must-revalidate").eTag(etag).body(clientList);
        }
    }

    @GetMapping(value = "/api/managed/client/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Client> getClient(
            @PathVariable Integer id,
            Authentication authentication) {
        Client client = clientService.getClient(authentication, id);

        if (client != null) {
            return ResponseEntity.ok().body(client);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/client", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Client> addClient(
            @RequestBody Client newClient,
            Authentication authentication) throws URISyntaxException {
        Client updatedClient = clientService.addClient(authentication, newClient);

        if (updatedClient != null) {
            return ResponseEntity
                .created(new URI("/api/managed/client/" + updatedClient.getId()))
                .body(updatedClient);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/client/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateClient(
            @PathVariable Integer id,
            @RequestBody Client newClient,
            Authentication authentication) {
        if (!id.equals(newClient.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Client targetClient = clientService.getClient(authentication, id);
        if (targetClient == null) {
            return ResponseEntity.notFound().build();
        }

        Client updatedClient = clientService.updateClient(authentication, targetClient, newClient);

        if (updatedClient != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/client/{id}")
    public ResponseEntity<?> removeClient(
            @PathVariable Integer id,
            Authentication authentication) {
        Client targetClient = clientService.getClient(authentication, id);
        if (targetClient == null) {
            return ResponseEntity.notFound().build();
        }

        Client deletedClient = clientService.removeClient(authentication, targetClient);

        if (deletedClient != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
