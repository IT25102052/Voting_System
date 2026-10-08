package com.slit.realityvote.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Security configuration for the Reality Vote platform.
 *
 * Configures DB-backed authentication, role-based access control,
 * SPA-compatible CSRF cookie generation, and structured access-denied handling.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/img/**",
                                 "/login", "/register", "/error/**", "/faq").permitAll()
                // Role-Based Access Control
                .requestMatchers("/admin/shows/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/admin/voting-sessions/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/admin/judges/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/staff/contestants/**").hasAnyRole("ADMINISTRATOR", "CONTESTANT_STAFF")
                .requestMatchers("/vote/**").hasRole("VIEWER")
                .requestMatchers("/judge/**").hasRole("JUDGE")
                .requestMatchers("/compliance/**").hasAnyRole("ADMINISTRATOR", "COMPLIANCE_OFFICER")
                .requestMatchers("/reports/**").hasAnyRole("ADMINISTRATOR", "REPORTING_MANAGER")
                .requestMatchers("/staff/support/**", "/staff/faqs/**").hasAnyRole("ADMINISTRATOR", "SUPPORT_STAFF")
                .requestMatchers("/support/tickets/**").hasRole("VIEWER")
                .requestMatchers("/marketing/**").hasAnyRole("ADMINISTRATOR", "MARKETING_OFFICER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String accept = request.getHeader("Accept");
                    String contentType = request.getHeader("Content-Type");
                    String requestedWith = request.getHeader("X-Requested-With");
                    boolean isAjaxOrApi = "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                            || (accept != null && accept.contains("application/json"))
                            || (contentType != null && contentType.contains("application/json"));
                    if (isAjaxOrApi) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access Denied: "
                                + (accessDeniedException.getMessage() != null
                                        ? accessDeniedException.getMessage().replace("\"", "\\\"")
                                        : "Forbidden") + "\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        request.getRequestDispatcher("/error/403").forward(request, response);
                    }
                })
            )
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }
}

/**
 * Custom CSRF token request handler supporting both XOR-encoded tokens (for forms)
 * and plain/unmasked tokens sent in AJAX/Fetch request headers (X-XSRF-TOKEN / X-CSRF-TOKEN).
 */
final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(headerValue)) ? headerValue : this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}

/**
 * Filter that forces deferred Spring Security 6 CSRF tokens to be resolved on every request,
 * guaranteeing that the XSRF-TOKEN cookie is set and accessible to client scripts.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
