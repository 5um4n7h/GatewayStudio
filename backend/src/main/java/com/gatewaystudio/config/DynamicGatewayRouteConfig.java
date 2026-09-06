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
        log.info("Building dynamic gateway RouterFunction from DynamicGatewayRouteSupplier");
        RouterFunction<ServerResponse> router = supplier.get();
        log.info("Dynamic gateway RouterFunction built successfully");
        return router;
    }
}
