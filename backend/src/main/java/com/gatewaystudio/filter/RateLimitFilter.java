package com.gatewaystudio.filter;

import com.gatewaystudio.entity.GatewayRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit filter for gateway routes.
 * Uses in-memory tracking with sliding window approach.
 */
public class RateLimitFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Map of (routeId + clientIp) -> list of request timestamps
    private static final Map<String, LinkedList<Long>> REQUEST_REGISTRY = new ConcurrentHashMap<>();

    public static boolean checkRateLimit(ServerRequest request, GatewayRoute route) {
        String clientIp = getClientIp(request);
        String key = route.getId() + ":" + clientIp;

        long now = System.currentTimeMillis();
        long windowStartMs = (route.getRateLimitWindowSeconds() * 1000L);

        LinkedList<Long> timestamps = REQUEST_REGISTRY.computeIfAbsent(key, k -> new LinkedList<>());

        // Remove old timestamps outside the window
        synchronized (timestamps) {
            timestamps.removeIf(timestamp -> (now - timestamp) > windowStartMs);

            // Check if we've exceeded the limit
            if (timestamps.size() >= route.getRateLimitRequests()) {
                log.warn("Rate limit exceeded for route={}, ip={}, limit={} requests per {} seconds",
                    route.getId(), clientIp, route.getRateLimitRequests(), route.getRateLimitWindowSeconds());
                return false;
            }

            // Add current request timestamp
            timestamps.add(now);
        }

        return true;
    }

    private static String getClientIp(ServerRequest request) {
        // Try X-Forwarded-For header first (for proxied requests)
        String forwardedIp = request.headers().firstHeader("X-Forwarded-For");
        if (forwardedIp != null && !forwardedIp.isEmpty()) {
            return forwardedIp.split(",")[0].trim();
        }

        return request.remoteAddress()
            .map(addr -> addr.getHostString())
            .orElse("unknown");
    }
}

