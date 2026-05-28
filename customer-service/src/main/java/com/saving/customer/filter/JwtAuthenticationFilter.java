package com.saving.customer.filter;

import com.saving.customer.common.Constants;
import com.saving.customer.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates JWT on every request, populates SecurityContext with username + roles.
 * Does NOT call Auth Service — parses JWT directly using shared secret.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(Constants.AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(Constants.BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(Constants.BEARER_PREFIX.length());

        try {
            if (jwtService.isTokenValid(token) && jwtService.isAccessToken(token)) {
                String       username = jwtService.extractUsername(token);
                List<String> roles    = jwtService.extractRoles(token);
                String       cif      = jwtService.extractCif(token);   // may be null for non-customer

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Store CIF as request attribute so controllers can verify ownership
                    if (cif != null) {
                        request.setAttribute(Constants.CLAIM_CIF, cif);
                    }

                    log.debug("JWT authenticated: user={}, cif={}, roles={}, path={}",
                            username, cif, roles, request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            log.warn("JWT filter error for path {}: {}", request.getRequestURI(), ex.getMessage());
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/api/v1/customers/internal/")   // internal calls use service token
                || path.equals("/api/v1/customers/health");
    }
}
