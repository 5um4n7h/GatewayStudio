package com.gatewaystudio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalRequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");
    private static final Logger log = LoggerFactory.getLogger(GlobalRequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        log.info("Incoming request: method={}, uri={}, requestId={}", method, requestUri, requestId);

        HttpServletRequestWrapper mutableRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                    return requestId;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(requestId));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                if (!names.contains(REQUEST_ID_HEADER)) {
                    names.add(REQUEST_ID_HEADER);
                }
                return Collections.enumeration(names);
            }
        };

        try {
            filterChain.doFilter(mutableRequest, response);
        } finally {
            long latency = System.currentTimeMillis() - startTime;

            Map<String, Object> auditData = new HashMap<>();
            auditData.put("path", requestUri);
            auditData.put("method", method);
            auditData.put("statusCode", response.getStatus());
            auditData.put("latencyMs", latency);
            auditData.put("clientIp", request.getRemoteAddr());
            auditData.put("userAgent", request.getHeader("User-Agent"));
            auditData.put("requestId", requestId);

            auditLogger.info(Markers.appendEntries(auditData), "HTTP Request Completed");
            log.info("Completed request: method={}, uri={}, status={}, latencyMs={}, requestId={}",
                    method, requestUri, response.getStatus(), latency, requestId);

            MDC.remove(MDC_KEY);
        }
    }
}
