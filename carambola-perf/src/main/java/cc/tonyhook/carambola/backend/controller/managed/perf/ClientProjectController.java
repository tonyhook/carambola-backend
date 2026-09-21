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

import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import cc.tonyhook.carambola.backend.service.perf.ClientProjectService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RequestMapping("/api/managed/perf/client-project")
@RestController
public class ClientProjectController {

    private final ClientProjectService clientProjectService;

    public ClientProjectController(ClientProjectService clientProjectService) {
        this.clientProjectService = clientProjectService;
    }

    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientProject>> getClientProjectList(@RequestParam(required = false) String query, Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<ClientProject> clientProjectList = clientProjectService.queryClientProjectList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(clientProjectList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<ClientProject> clientProjectList = clientProjectService.getClientProjectList(authentication);

            return ResponseEntity.ok().body(clientProjectList);
        }
    }

    @GetMapping(value = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ClientProject> getClientProject(@PathVariable Integer id, Authentication authentication) {
        ClientProject clientProject = clientProjectService.getClientProject(authentication, id);

        if (clientProject != null) {
            return ResponseEntity.ok().body(clientProject);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = "application/json; charset=UTF-8")
    public ResponseEntity<ClientProject> addClientProject(@RequestBody ClientProject newClientProject, Authentication authentication) throws URISyntaxException {
        ClientProject updatedClientProject = clientProjectService.addClientProject(authentication, newClientProject);

        if (updatedClientProject != null) {
            return ResponseEntity
                .created(new URI("/api/managed/perf/client-project/" + updatedClientProject.getId()))
                .body(updatedClientProject);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateClientProject(@PathVariable Integer id, @RequestBody ClientProject newClientProject, Authentication authentication) {
        if (!id.equals(newClientProject.getId())) {
            return ResponseEntity.badRequest().build();
        }

        ClientProject targetClientProject = clientProjectService.getClientProject(authentication, id);
        if (targetClientProject == null) {
            return ResponseEntity.notFound().build();
        }

        ClientProject updatedClientProject = clientProjectService.updateClientProject(authentication, targetClientProject, newClientProject);

        if (updatedClientProject != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeClientProject(@PathVariable Integer id, Authentication authentication) {
        ClientProject targetClientProject = clientProjectService.getClientProject(authentication, id);
        if (targetClientProject == null) {
            return ResponseEntity.notFound().build();
        }

        ClientProject deletedClientProject = clientProjectService.removeClientProject(authentication, targetClientProject);

        if (deletedClientProject != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
