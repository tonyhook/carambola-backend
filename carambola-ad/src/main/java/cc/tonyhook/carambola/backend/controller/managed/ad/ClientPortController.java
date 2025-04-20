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
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.service.ad.ClientMediaService;
import cc.tonyhook.carambola.backend.service.ad.ClientPortService;
import cc.tonyhook.carambola.backend.service.ad.ClientService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class ClientPortController {

    private final ClientService clientService;
    private final ClientMediaService clientMediaService;
    private final ClientPortService clientPortService;

    public ClientPortController(ClientService clientService, ClientMediaService clientMediaService, ClientPortService clientPortService) {
        this.clientService = clientService;
        this.clientMediaService = clientMediaService;
        this.clientPortService = clientPortService;
    }

    @GetMapping(value = "/api/managed/clientport", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientPort>> getClientPortList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<ClientPort> clientPortList = clientPortService.queryClientPortList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(clientPortList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication);

            return ResponseEntity.ok().body(clientPortList);
        }
    }

    @GetMapping(value = "/api/managed/clientport/client/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientPort>> getClientPortListByClient(
            @PathVariable Integer id,
            Authentication authentication) {
        Client client = clientService.getClient(authentication, id);
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication, client);

        return ResponseEntity.ok().body(clientPortList);
    }

    @GetMapping(value = "/api/managed/clientport/clientmedia/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientPort>> getClientPortListByClientMedia(
            @PathVariable Integer id,
            Authentication authentication) {
        ClientMedia clientMedia = clientMediaService.getClientMedia(authentication, id);
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication, clientMedia);

        return ResponseEntity.ok().body(clientPortList);
    }

    @GetMapping(value = "/api/managed/clientport/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ClientPort> getClientPort(
            @PathVariable Integer id,
            Authentication authentication) {
        ClientPort clientPort = clientPortService.getClientPort(authentication, id);

        if (clientPort != null) {
            return ResponseEntity.ok().body(clientPort);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/clientport", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<ClientPort> addClientPort(
            @RequestBody ClientPort newClientPort,
            Authentication authentication) throws URISyntaxException {
        ClientPort updatedClientPort = clientPortService.addClientPort(authentication, newClientPort);

        if (updatedClientPort != null) {
            return ResponseEntity
                .created(new URI("/api/managed/clientport/" + updatedClientPort.getId()))
                .body(updatedClientPort);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/clientport/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateClientPort(
            @PathVariable Integer id,
            @RequestBody ClientPort newClientPort,
            Authentication authentication) {
        if (!id.equals(newClientPort.getId())) {
            return ResponseEntity.badRequest().build();
        }

        ClientPort targetClientPort = clientPortService.getClientPort(authentication, id);
        if (targetClientPort == null) {
            return ResponseEntity.notFound().build();
        }

        ClientPort updatedClientPort = clientPortService.updateClientPort(authentication, targetClientPort, newClientPort);

        if (updatedClientPort != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/clientport/{id}")
    public ResponseEntity<?> removeClientPort(
            @PathVariable Integer id,
            Authentication authentication) {
        ClientPort targetClientPort = clientPortService.getClientPort(authentication, id);
        if (targetClientPort == null) {
            return ResponseEntity.notFound().build();
        }

        ClientPort deletedClientPort = clientPortService.removeClientPort(authentication, targetClientPort);

        if (deletedClientPort != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
