package com.gatewaystudio.controller;


import com.gatewaystudio.entity.GatewayRoute;
import com.gatewaystudio.repository.JpaGatewayRoute;
import com.gatewaystudio.service.DynamicGatewayRouteSupplier;
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
    private final DynamicGatewayRouteSupplier routeSupplier;

    public DynamicRouteAdminController(JpaGatewayRoute repository,
                                       @Qualifier("configDataContextRefresher") ContextRefresher contextRefresher, DynamicGatewayRouteSupplier routeSupplier) {
        this.repository = repository;
        this.contextRefresher = contextRefresher;
        this.routeSupplier = routeSupplier;
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
        routeSupplier.refreshTenantRoutes(tenantId); // Refresh routes for this tenant
        log.info("Triggered context refresh after route creation.");

        return ResponseEntity.ok(savedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@RequestHeader(value = "X-Tenant-ID", defaultValue = "TNT001") String tenantId, @PathVariable String id) {
        log.info("Deleting route with id={}", id);
        repository.deleteById(id);

        contextRefresher.refresh();
        routeSupplier.refreshTenantRoutes(tenantId);
        log.info("Triggered context refresh after route deletion for id={}", id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enableOrDisableRoute")
    public ResponseEntity<Void> enableOrDisableRoute(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "TNT001") String tenantId) {

        var routes = repository.findByTenantIdAndId(tenantId, id);
        if (routes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GatewayRoute route = routes.get(0);
        route.setEnabled(!route.isEnabled());
        repository.save(route);

        contextRefresher.refresh();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<String>> getAllTenants() {
        log.info("Fetching all available tenants");
        // Get all unique tenant IDs from database
        List<String> tenants = repository.findDistinctTenantIds();
        log.info("Found {} unique tenants", tenants.size());
        return ResponseEntity.ok(tenants);
    }

    // inside DynamicRouteAdminController class
    @PostMapping("/enableAll")
    public ResponseEntity<Void> setAllRoutesEnabled(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "TNT001") String tenantId,
            @RequestParam("enabled") boolean enabled) {

        log.info("Setting enabled={} for all routes of tenant={}", enabled, tenantId);

        int updated = repository.updateEnabledByTenant(tenantId, enabled);
        log.info("Updated {} routes for tenant={}", updated, tenantId);
        contextRefresher.refresh();
        routeSupplier.refreshTenantRoutes(tenantId);

        log.info("Completed setAllRoutesEnabled for tenant={}", tenantId);
        return ResponseEntity.ok().build();
    }

}
