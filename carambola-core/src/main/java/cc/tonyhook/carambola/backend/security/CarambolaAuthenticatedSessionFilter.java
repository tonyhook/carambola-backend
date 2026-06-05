package cc.tonyhook.carambola.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cc.tonyhook.carambola.backend.dao.security.UserRepository;
import cc.tonyhook.carambola.backend.entity.security.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CarambolaAuthenticatedSessionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public CarambolaAuthenticatedSessionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof UserDetails userDetails) {
            HttpSession session = request.getSession();

            if (session.getAttribute("id") == null || session.getAttribute("username") == null) {
                User user = userRepository.findByUsername(userDetails.getUsername());
                if (user != null) {
                    session.setAttribute("id", user.getId());
                    session.setAttribute("username", userDetails.getUsername());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

}
