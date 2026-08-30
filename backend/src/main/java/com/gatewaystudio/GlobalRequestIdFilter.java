package com.gatewaystudio;

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
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensures execution before Gateway routing logic
public class GlobalRequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 1. Generate a brand new, unique UUID for this specific thread/request
        String requestId = UUID.randomUUID().toString();

        // 2. Put it in MDC so your current Gateway console logs print this ID automatically
        MDC.put(MDC_KEY, requestId);

        // 3. Optional: Send the same ID back to the calling client in the response header
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // 4. Wrap and mutate the HTTP request to forward the header downstream
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
            // 5. Hand the mutated request off to Spring Cloud Gateway Server MVC
            filterChain.doFilter(mutableRequest, response);
        } finally {
            // 2. Executes ONLY after request is fully processed
            long latency = System.currentTimeMillis() - startTime;

            Map<String, Object> auditData = new HashMap<>();
            auditData.put("path", request.getRequestURI());
            auditData.put("method", request.getMethod());
            auditData.put("statusCode", response.getStatus());
            auditData.put("latencyMs", latency);
            auditData.put("clientIp", request.getRemoteAddr());
            auditData.put("userAgent", request.getHeader("User-Agent"));

            // Writes structured JSON line asynchronously to logs/audit.log
            auditLogger.info(Markers.appendEntries(auditData), "HTTP Request Completed");


            MDC.remove(MDC_KEY);


        }
    }
}
