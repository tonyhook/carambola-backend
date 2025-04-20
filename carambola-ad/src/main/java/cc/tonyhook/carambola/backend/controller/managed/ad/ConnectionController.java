package cc.tonyhook.carambola.backend.controller.managed.ad;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.ConnectionLog;
import cc.tonyhook.carambola.backend.service.ad.ConnectionLogService;
import cc.tonyhook.carambola.backend.service.ad.ConnectionService;
import jakarta.transaction.Transactional;

@RequestMapping("/api/managed/ad/connection")
@RestController
public class ConnectionController {

    private final ConnectionService connectionService;
    private final ConnectionLogService connectionLogService;

    public ConnectionController(ConnectionService connectionService, ConnectionLogService connectionLogService) {
        this.connectionService = connectionService;
        this.connectionLogService = connectionLogService;
    }

    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Connection>> getConnectionList(
            Authentication authentication) {
        Boolean authenticated = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("AD_MANAGEMENT") || authority.getAuthority().equals("AD_OPERATION")) {
                authenticated = true;
            }
        }

        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Connection> connectionList = connectionService.getConnectionList();

        return ResponseEntity.ok().body(connectionList);
    }

    @GetMapping(value = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Connection> getConnection(
            @PathVariable Integer id,
            Authentication authentication) {
        Boolean authenticated = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("AD_MANAGEMENT") || authority.getAuthority().equals("AD_OPERATION")) {
                authenticated = true;
            }
        }

        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Connection connection = connectionService.getConnection(id);

        if (connection != null) {
            return ResponseEntity.ok().body(connection);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Connection> addConnection(
            @RequestBody Connection newConnection,
            Authentication authentication) throws URISyntaxException {
        Boolean authenticated = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("AD_MANAGEMENT") || authority.getAuthority().equals("AD_OPERATION")) {
                authenticated = true;
            }
        }

        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Connection updatedConnection = connectionService.addConnection(newConnection);

        return ResponseEntity
                .created(new URI("/api/managed/ad/connection/" + updatedConnection.getId()))
                .body(updatedConnection);
    }

    @PutMapping(value = "/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateConnection(
            @PathVariable Integer id,
            @RequestBody Connection newConnection,
            Authentication authentication) {
        Boolean authenticated = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("AD_MANAGEMENT") || authority.getAuthority().equals("AD_OPERATION")) {
                authenticated = true;
            }
        }

        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!id.equals(newConnection.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Connection targetConnection = connectionService.getConnection(id);
        if (targetConnection == null) {
            return ResponseEntity.notFound().build();
        }

        if (targetConnection.getEnabled() != newConnection.getEnabled()) {
            ConnectionLog connectionLog = new ConnectionLog();
            connectionLog.setConnection(targetConnection);
            connectionLog.setEnabled(newConnection.getEnabled());
            connectionLog.setTime(new Timestamp(System.currentTimeMillis()));
            connectionLogService.addConnectionLog(connectionLog);
        }

        connectionService.updateConnection(id, newConnection);

        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeConnection(
            @PathVariable Integer id,
            Authentication authentication) {
        Boolean authenticated = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("AD_MANAGEMENT") || authority.getAuthority().equals("AD_OPERATION")) {
                authenticated = true;
            }
        }

        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Connection deletedConnection = connectionService.getConnection(id);
        if (deletedConnection == null) {
            return ResponseEntity.notFound().build();
        }

        connectionService.removeConnection(id);

        return ResponseEntity.ok().build();
    }

}
