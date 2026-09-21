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

import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.service.perf.ClientChannelService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RequestMapping("/api/managed/perf/client-channel")
@RestController
public class ClientChannelController {

    private final ClientChannelService clientChannelService;

    public ClientChannelController(ClientChannelService clientChannelService) {
        this.clientChannelService = clientChannelService;
    }

    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientChannel>> getClientChannelList(@RequestParam(required = false) String query, Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<ClientChannel> clientChannelList = clientChannelService.queryClientChannelList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(clientChannelList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<ClientChannel> clientChannelList = clientChannelService.getClientChannelList(authentication);

            return ResponseEntity.ok().body(clientChannelList);
        }
    }

    @GetMapping(value = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ClientChannel> getClientChannel(@PathVariable Integer id, Authentication authentication) {
        ClientChannel clientChannel = clientChannelService.getClientChannel(authentication, id);

        if (clientChannel != null) {
            return ResponseEntity.ok().body(clientChannel);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = "application/json; charset=UTF-8")
    public ResponseEntity<ClientChannel> addClientChannel(@RequestBody ClientChannel newClientChannel, Authentication authentication) throws URISyntaxException {
        ClientChannel updatedClientChannel = clientChannelService.addClientChannel(authentication, newClientChannel);

        if (updatedClientChannel != null) {
            return ResponseEntity
                .created(new URI("/api/managed/perf/client-channel/" + updatedClientChannel.getId()))
                .body(updatedClientChannel);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateClientChannel(@PathVariable Integer id, @RequestBody ClientChannel newClientChannel, Authentication authentication) {
        if (!id.equals(newClientChannel.getId())) {
            return ResponseEntity.badRequest().build();
        }

        ClientChannel targetClientChannel = clientChannelService.getClientChannel(authentication, id);
        if (targetClientChannel == null) {
            return ResponseEntity.notFound().build();
        }

        ClientChannel updatedClientChannel = clientChannelService.updateClientChannel(authentication, targetClientChannel, newClientChannel);

        if (updatedClientChannel != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeClientChannel(@PathVariable Integer id, Authentication authentication) {
        ClientChannel targetClientChannel = clientChannelService.getClientChannel(authentication, id);
        if (targetClientChannel == null) {
            return ResponseEntity.notFound().build();
        }

        ClientChannel deletedClientChannel = clientChannelService.removeClientChannel(authentication, targetClientChannel);

        if (deletedClientChannel != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
