package com.gatewaystudio.controller;


import com.gatewaystudio.entity.GatewayRoute;
import com.gatewaystudio.repository.JpaGatewayRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/routes")
public class DynamicRouteAdminController {

    private static final Logger log = LoggerFactory.getLogger(DynamicRouteAdminController.class);

    private final JpaGatewayRoute repository;
    private final ContextRefresher contextRefresher;

    public DynamicRouteAdminController(JpaGatewayRoute repository,
                                      @Qualifier("configDataContextRefresher") ContextRefresher contextRefresher) {
        this.repository = repository;
        this.contextRefresher = contextRefresher;
    }

    @GetMapping
    public List<GatewayRoute> getRoutes(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "TNT001") String tenantId) {
        log.info("Fetching routes for tenantId={}", tenantId);
        List<GatewayRoute> routes = repository.findByTenantId(tenantId);
        log.info("Found {} routes for tenantId={}", routes.size(), tenantId);
        return routes;
    }

    @PostMapping
    public ResponseEntity<GatewayRoute> createRoute(
            @RequestHeader(value = "X-Tenant-ID") String tenantId,
            @RequestBody GatewayRoute route) {

        if (tenantId != null && !tenantId.isBlank()) {
            route.setTenantId(tenantId);
        }

        log.info("Creating route: tenantId={}, route={}", tenantId, route);

        GatewayRoute savedRoute = repository.save(route);
        log.info("Saved route: {}", savedRoute);

        contextRefresher.refresh();
        log.info("Triggered context refresh after route creation.");

        return ResponseEntity.ok(savedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable String id) {
        log.info("Deleting route with id={}", id);
        repository.deleteById(id);

        contextRefresher.refresh();
        log.info("Triggered context refresh after route deletion for id={}", id);

        return ResponseEntity.noContent().build();
    }
}
