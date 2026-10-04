package com.gatewaystudio.config;

import com.gatewaystudio.service.DynamicGatewayRouteSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class DynamicGatewayRouteConfig {
    private static final Logger log = LoggerFactory.getLogger(DynamicGatewayRouteConfig.class);

    @Bean
    public RouterFunction<ServerResponse> dynamicGatewayRoutes(DynamicGatewayRouteSupplier supplier) {
        log.info("Building lazy-loading dynamic gateway RouterFunction");

        // Return a router that loads routes per tenant on each request
        return (request) -> {
            String tenantId = request.headers().firstHeader("X-Tenant-ID");
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "TNT001"; // default fallback
            }

            log.debug("Processing request for tenantId: {}", tenantId);

            // Get or load routes for this tenant
            RouterFunction<ServerResponse> tenantRouter = supplier.getRouterForTenant(tenantId);
            return tenantRouter.route(request);
        };
    }
}
