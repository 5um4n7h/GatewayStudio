package com.gatewaystudio;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTimeoutTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer wireMockServer;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void resetStubs() {
        wireMockServer.resetAll();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void shouldReturn200ForNormalResponse() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Valid Product\"}")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/product/1"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Valid Product");
    }

    @Test
    void shouldReturn500WhenUpstreamFailsWithInternalServerError() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/500"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/product/500"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
    }

    @Test
    void shouldTimeoutWhenUpstreamIsDelayedBeyondThreshold() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(4000)
                        .withBody("{\"id\": 99, \"name\": \"Slow Product\"}")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/product/slow"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(504);
    }

    @Test
    void shouldHandleNoResponseFaultFromUpstream() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/fault"))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.EMPTY_RESPONSE)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/product/fault"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
    }
}