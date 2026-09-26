package com.gatewaystudio.service;

import com.gatewaystudio.entity.GatewayRoute;
import com.gatewaystudio.repository.JpaGatewayRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.function.Supplier;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;

@Component
public class DynamicGatewayRouteSupplier implements Supplier<RouterFunction<ServerResponse>> {

    private static final Logger log = LoggerFactory.getLogger(DynamicGatewayRouteSupplier.class);

    private final JpaGatewayRoute repository;

    public DynamicGatewayRouteSupplier(JpaGatewayRoute repository) {
        this.repository = repository;
    }

    @Override
    public RouterFunction<ServerResponse> get() {
        RouterFunctions.Builder builder = RouterFunctions.route();
        List<GatewayRoute> activeRoutes = repository.findByEnabledTrue();

        log.info("Loading dynamic gateway routes from database. Active route count: {}", activeRoutes.size());

        for (GatewayRoute route : activeRoutes) {
            log.info("Registering route: id={}, name={}, publicPath={}, targetUri={}, stripPrefix={}, enabled={}",
                    route.getId(), route.getRouteName(), route.getPublicPath(), route.getTargetUri(),
                    route.getStripPrefix(), route.isEnabled());

            var routeBuilder = GatewayRouterFunctions.route(route.getId())
                    .route(
                            GatewayRequestPredicates.path(route.getPublicPath()),
                            HandlerFunctions.http(route.getTargetUri())
                    );

            if (route.getStripPrefix() > 0) {
                log.info("Applying stripPrefix={} for route {}", route.getStripPrefix(), route.getId());
                routeBuilder = routeBuilder.filter(stripPrefix(route.getStripPrefix()));
            }

            builder.add(routeBuilder.build());
        }

        // If no routes are configured, add a default health check route to prevent build() failure
        if (activeRoutes.isEmpty()) {
            log.warn("No active routes found in database. Adding default health check route.");
            builder.add(
                RouterFunctions.route()
                    .GET("/health", request -> ServerResponse.ok()
                        .body("Gateway is running. No routes configured. Please add routes via API."))
                    .build()
            );
        }

        log.info("Dynamic gateway route registration complete. Built router with {} routes.", activeRoutes.size());
        return builder.build();
    }
}