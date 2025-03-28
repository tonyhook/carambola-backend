package cc.tonyhook.carambola.backend.web;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CarambolaContentCachingFilter extends OncePerRequestFilter {

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CarambolaCachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CarambolaCachedBodyHttpServletRequest(request);
        CarambolaCachedBodyHttpServletResponse cachedBodyHttpServletResponse = new CarambolaCachedBodyHttpServletResponse(response);
        filterChain.doFilter(cachedBodyHttpServletRequest, cachedBodyHttpServletResponse);
    }

}
