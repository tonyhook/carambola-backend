package cc.tonyhook.carambola.backend.controller.open;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenSecurityController {

    @GetMapping(value = "/api/open/security/user-details", produces = "application/json; charset=UTF-8")
    public UserDetails getUserDetails() {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return userDetails;
        } catch (Exception e) {
            return null;
        }
    }

}
