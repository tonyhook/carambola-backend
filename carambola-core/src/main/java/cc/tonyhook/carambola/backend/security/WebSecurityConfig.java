package cc.tonyhook.carambola.backend.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.JdbcUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebSecurityConfig {

    private final DataSource securityDataSource;
    private final CarambolaAuthenticationSuccessHandler authenticationSuccessHandler;
    private final CarambolaAuthenticationFailureHandler authenticationFailureHandler;
    private final CarambolaAccessDeniedHandler accessDeniedHandler;
    private final CarambolaAuthenticationEntryPoint authenticationEntryPoint;
    private final CarambolaAuthenticatedSessionFilter authenticatedSessionFilter;

    public WebSecurityConfig(
            DataSource securityDataSource,
            CarambolaAuthenticationSuccessHandler authenticationSuccessHandler,
            CarambolaAuthenticationFailureHandler authenticationFailureHandler,
            CarambolaAccessDeniedHandler accessDeniedHandler,
            CarambolaAuthenticationEntryPoint authenticationEntryPoint,
            CarambolaAuthenticatedSessionFilter authenticatedSessionFilter
    ) {
        this.securityDataSource = securityDataSource;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticatedSessionFilter = authenticatedSessionFilter;
    }

    @Bean
    public UserDetailsManager configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        JdbcUserDetailsManagerConfigurer<AuthenticationManagerBuilder> jdbcUserDetailsManagerConfigurer = auth
            .jdbcAuthentication()
                .dataSource(securityDataSource)
                .usersByUsernameQuery(
                    "SELECT username, password, enabled from sys_user where username=?")
                .authoritiesByUsernameQuery(
                    "SELECT " +
                    "    username, sys_authority.name AS authority " +
                    "FROM " +
                    "    sys_user, sys_user_roles, sys_role_authorities, sys_authority " +
                    "WHERE " +
                    "    username = ? " +
                    "        AND sys_user.id = sys_user_roles.user_id " +
                    "        AND sys_user_roles.roles_id = sys_role_authorities.role_id " +
                    "        AND sys_role_authorities.authorities_id = sys_authority.id");

        return jdbcUserDetailsManagerConfigurer.getUserDetailsService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers("/api/managed/**").authenticated()
                .anyRequest().permitAll())
            .formLogin(formLogin -> formLogin
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler))
            .logout(logout -> logout
                .logoutSuccessHandler(authenticationSuccessHandler))
            .rememberMe(rememberMe -> rememberMe
                .key("carambola-remember-me")
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(60 * 60 * 24 * 30))
            .cors(withDefaults())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/open/**")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint))
            .addFilterAfter(authenticatedSessionFilter, RememberMeAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            csrfToken.getToken();

            filterChain.doFilter(request, response);
        }

    }

}
