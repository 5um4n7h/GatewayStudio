package com.gatewaystudio;


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

    @Bean
    public RestClientProxyExchange restClientProxyExchange(
            RestClient.Builder restClientBuilder,
            GatewayMvcProperties properties) {

        // 1. Create Java 11+ HttpClient
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)) // TCP Connection Timeout
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // 2. Wrap it with JdkClientHttpRequestFactory and set the Read/Socket Timeout
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2)); // Upstream response timeout (2 Seconds)

        // 3. Build a custom RestClient used strictly for Gateway Proxy Routing
        RestClient gatewayRestClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

        return new RestClientProxyExchange(gatewayRestClient, properties);
    }
}