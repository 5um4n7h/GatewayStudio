package com.gatewaystudio.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;

import org.springframework.cloud.gateway.server.mvc.handler.RestClientProxyExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class GatewayProxyConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyConfig.class);

    @Bean
    public RestClientProxyExchange restClientProxyExchange(
            RestClient.Builder restClientBuilder,
            GatewayMvcProperties properties) {

        log.info("Initializing custom gateway proxy client with connectTimeout=3s and readTimeout=2s");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)) // TCP Connection Timeout
                .version(HttpClient.Version.HTTP_2)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2)); // Upstream response timeout (2 Seconds)

        RestClient gatewayRestClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

        log.info("Gateway proxy client initialized successfully.");
        return new RestClientProxyExchange(gatewayRestClient, properties);
    }
}