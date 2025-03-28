package cc.tonyhook.carambola.backend.controller.managed.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.security.User;
import cc.tonyhook.carambola.backend.service.security.UserService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/api/managed/user", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<User>> getUserList(
            @RequestParam(required = false) String query) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<User> userList = userService.queryUserList(objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(userList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<User> userList = userService.getUserList();

            return ResponseEntity.ok().body(userList);
        }
    }

    @GetMapping(value = "/api/managed/user/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<User> getUser(
            @PathVariable Integer id) {
        User user = userService.getUser(id);

        if (user != null) {
            return ResponseEntity.ok().body(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/user", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<User> addUser(
            @RequestBody User newUser) throws URISyntaxException {
        newUser.setPassword(BCrypt.hashpw(newUser.getPassword(), BCrypt.gensalt()));

        User updatedUser = userService.addUser(newUser);

        return ResponseEntity
                .created(new URI("/api/managed/user/" + updatedUser.getId()))
                .body(updatedUser);
    }

    @PutMapping(value = "/api/managed/user/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateUser(
            @PathVariable Integer id,
            @RequestBody User newUser) {
        if (!id.equals(newUser.getId())) {
            return ResponseEntity.badRequest().build();
        }

        User targetUser = userService.getUser(id);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }

        if (newUser.getPassword() == null) {
            newUser.setPassword(targetUser.getPassword());
        } else {
            newUser.setPassword(BCrypt.hashpw(newUser.getPassword(), BCrypt.gensalt()));
        }
        userService.updateUser(id, newUser);

        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/api/managed/user/{id}")
    public ResponseEntity<?> removeUser(
            @PathVariable Integer id) {
        User deletedUser = userService.getUser(id);
        if (deletedUser == null) {
            return ResponseEntity.notFound().build();
        }

        userService.removeUser(id);

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/api/managed/user", consumes = "text/plain; charset=UTF-8")
    public ResponseEntity<?> updatePassword(
            HttpServletRequest request,
            @RequestBody String password) {
        Integer id = (Integer) request.getSession().getAttribute("id");

        User targetUser = userService.getUser(id);
        if (targetUser == null) {
            return ResponseEntity.badRequest().build();
        }

        targetUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));

        userService.updateUser(id, targetUser);

        return ResponseEntity.ok().build();
    }

}
