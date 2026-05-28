package com.saving.account.filter;

import com.saving.account.common.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(Constants.CORRELATION_ID_HEADER);

        if (!StringUtils.hasText(correlationId)) {
            correlationId = "ACC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }

        MDC.put(Constants.CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(Constants.CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(Constants.CORRELATION_ID_MDC_KEY);
        }
    }
}
