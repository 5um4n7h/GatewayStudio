package com.gatewaystudio.filter;

import com.gatewaystudio.entity.GatewayRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * Payload size validator filter for gateway routes.
 * Checks Content-Length header against configured maximum.
 */
public class PayloadSizeFilter {
    private static final Logger log = LoggerFactory.getLogger(PayloadSizeFilter.class);

    public static boolean checkPayloadSize(ServerRequest request, GatewayRoute route) {
        // Get Content-Length header
        String contentLengthStr = request.headers().firstHeader("Content-Length");

        if (contentLengthStr == null || contentLengthStr.isEmpty()) {
            // No Content-Length header, allow it to proceed (streaming might be used)
            return true;
        }

        try {
            long contentLengthBytes = Long.parseLong(contentLengthStr);
            long maxSizeBytes = route.getMaxPayloadSizeMb() * 1024L * 1024L;

            if (contentLengthBytes > maxSizeBytes) {
                log.warn("Payload size exceeded for route={}, size={} bytes, max={} MB ({} bytes)",
                    route.getId(), contentLengthBytes, route.getMaxPayloadSizeMb(), maxSizeBytes);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid Content-Length header for route={}: {}", route.getId(), contentLengthStr);
            return false;
        }

        return true;
    }
}

