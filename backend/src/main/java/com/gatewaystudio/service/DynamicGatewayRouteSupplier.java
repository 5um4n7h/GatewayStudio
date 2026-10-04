package com.gatewaystudio.service;

import com.gatewaystudio.entity.GatewayRoute;
import com.gatewaystudio.filter.PayloadSizeFilter;
import com.gatewaystudio.filter.RateLimitFilter;
import com.gatewaystudio.repository.JpaGatewayRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class DynamicGatewayRouteSupplier implements Supplier<RouterFunction<ServerResponse>> {

    private static final Logger log = LoggerFactory.getLogger(DynamicGatewayRouteSupplier.class);
    private final Map<String, RouterFunction<ServerResponse>> tenantRouterCache = new ConcurrentHashMap<>();
    private final JpaGatewayRoute repository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DynamicGatewayRouteSupplier(JpaGatewayRoute repository) {
        this.repository = repository;
    }

    @Override
    public RouterFunction<ServerResponse> get() {
        // Legacy method for backward compatibility - loads default tenant
        return getRouterForTenant("TNT001");
    }

    /**
     * Get or load routes for a specific tenant (lazy-loaded on first request)
     */
    public RouterFunction<ServerResponse> getRouterForTenant(String tenantId) {
        return tenantRouterCache.computeIfAbsent(tenantId, tid -> {
            log.info("Building routes for tenant: {}", tid);
            return buildRouterForTenant(tid);
        });
    }

    /**
     * Build the actual router for a tenant
     */
    private RouterFunction<ServerResponse> buildRouterForTenant(String tenantId) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        List<GatewayRoute> activeRoutes = repository.findByTenantId(tenantId);

        log.info("Loading dynamic gateway routes for tenant: {}. Active route count: {}",
                tenantId, activeRoutes.size());

        for (GatewayRoute route : activeRoutes) {
            log.info("Registering route for tenant {}: id={}, name={}, publicPath={}",
                    tenantId, route.getId(), route.getRouteName(), route.getPublicPath());

            var routeBuilder = GatewayRouterFunctions.route(route.getId())
                    .route(
                            GatewayRequestPredicates.path(route.getPublicPath()),
                            request -> forwardRequest(request, route)
                    );

            builder.add(routeBuilder.build());
        }

        if (activeRoutes.isEmpty()) {
            log.warn("No active routes found for tenant: {}. Adding fallback health route.", tenantId);
            builder.add(
                    RouterFunctions.route()
                            .GET("/health", request -> ServerResponse.ok()
                                    .body("Gateway is running for tenant: " + tenantId + ". No routes configured."))
                            .build()
            );
        }
        log.info("Dynamic gateway route registration complete for tenant: {}. Built router with {} routes.",
                tenantId, activeRoutes.size());
        return builder.build();
    }

    /**
     * Refresh routes for a specific tenant (call this when routes are updated via admin API)
     */
    public void refreshTenantRoutes(String tenantId) {
        log.info("Refreshing routes for tenant: {}", tenantId);
        tenantRouterCache.remove(tenantId);
        getRouterForTenant(tenantId); // Pre-load the refreshed routes
    }


    private ServerResponse forwardRequest(ServerRequest request, GatewayRoute route) {

        if (!route.isEnabled()) {
            log.warn("Blocked request for disabled route: {}", route.getId());
            return ServerResponse.status(503)
                    .body("Service Unavailable: Route is disabled.");
        }

        if (!RateLimitFilter.checkRateLimit(request, route)) {
            return ServerResponse.status(429)
                    .body("Rate limit exceeded: " + route.getRateLimitRequests() +
                            " requests per " + route.getRateLimitWindowSeconds() + " seconds");
        }

        if (!PayloadSizeFilter.checkPayloadSize(request, route)) {
            return ServerResponse.status(413)
                    .body("Payload too large. Maximum size: " + route.getMaxPayloadSizeMb() + " MB");
        }

        String upstreamUrl = buildUpstreamUrl(request, route);
        log.info("Forwarding {} {} -> {}", request.methodName(), request.path(), upstreamUrl);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(upstreamUrl));

            String method = request.methodName();
            String forwardedFor = request.headers().firstHeader("X-Forwarded-For");
            if (forwardedFor == null || forwardedFor.isBlank()) {
                forwardedFor = "unknown";
            }
            builder.header("X-Forwarded-For", forwardedFor);

            String host = request.headers().firstHeader("Host");
            if (host != null && !host.isBlank()) {
                builder.header("X-Forwarded-Host", host);
            } else {
                builder.header("X-Forwarded-Host", "localhost");
            }


            if ("GET".equalsIgnoreCase(method)) {
                builder.GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = request.body(String.class);
                builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else if ("PUT".equalsIgnoreCase(method)) {
                String body = request.body(String.class);
                builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                builder.DELETE();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpRequest httpRequest = builder.build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            String contentType = request.headers().firstHeader(HttpHeaders.CONTENT_TYPE);
            if (contentType != null && !contentType.isBlank()) {
                builder.header(HttpHeaders.CONTENT_TYPE, contentType);
            } else {
                builder.header(HttpHeaders.CONTENT_TYPE, "application/json");
            }

            return ServerResponse
                    .status(response.statusCode())
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(responseBody);

        } catch (Exception e) {
            log.error("Proxy error for route {} to {}", route.getId(), upstreamUrl, e);
            return ServerResponse.status(502)
                    .body("Bad Gateway: " + e.getMessage());
        }
    }

    private String buildUpstreamUrl(ServerRequest request, GatewayRoute route) {
        String target = route.getTargetUri().replaceAll("/+$", "");
        String reqPath = request.path();

        String suffix = reqPath;

        if (route.getStripPrefix() > 0) {
            String[] parts = reqPath.split("/");
            StringBuilder rebuilt = new StringBuilder();

            for (int i = route.getStripPrefix() + 1; i < parts.length; i++) {
                if (parts[i] != null && !parts[i].isBlank()) {
                    rebuilt.append("/").append(parts[i]);
                }
            }

            suffix = rebuilt.length() == 0 ? "/" : rebuilt.toString();
        }

        if (suffix == null || suffix.isBlank() || "/".equals(suffix)) {
            return target;
        }

        return target + suffix;
    }
}