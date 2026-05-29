package com.saving.product.filter;

import com.saving.product.common.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every inbound HTTP request and its response.
 *
 * Sample output (MDC already has correlationId from CorrelationIdFilter):
 *
 *   ► GET /api/v1/products | ip: 10.244.0.1 | user: teller01
 *   ... (service-layer step logs) ...
 *   ◄ 200 GET /api/v1/products | 45ms
 *
 * Order(2) — runs after CorrelationIdFilter (Order 1).
 */
@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String[] SKIP_PATHS = {
            "/actuator", "/api/v1/products/health"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path   = request.getRequestURI();
        String ip     = resolveClientIp(request);

        log.info("► {} {} | ip: {}", method, path, ip);
        long start = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long   ms     = System.currentTimeMillis() - start;
            int    status = response.getStatus();
            String label  = String.format("◄ %d %s %s | %dms", status, method, path, ms);

            MDC.remove(Constants.MDC_USERNAME_KEY);

            if (status >= 500) {
                log.error(label);
            } else if (status >= 400) {
                log.warn(label);
            } else {
                log.info(label);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String skip : SKIP_PATHS) {
            if (path.startsWith(skip)) return true;
        }
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
