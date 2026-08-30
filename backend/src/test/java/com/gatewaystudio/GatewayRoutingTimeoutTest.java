package com.gatewaystudio;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTimeoutTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer wireMockServer;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        // Construct standard Spring RestClient targeting the test port
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private HttpStatusCode getStatus(String path) {
        return restClient.get()
                .uri(path)
                .exchange((request, response) -> response.getStatusCode());
    }

    @Test
    void shouldReturn200ForNormalResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Valid Product\"}")));

        HttpStatusCode status = getStatus("/product/1");
        assertThat(status.value()).isEqualTo(200);
    }

    @Test
    void shouldReturn500WhenUpstreamFailsWithInternalServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/500"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        HttpStatusCode status = getStatus("/product/500");
        assertThat(status.is5xxServerError()).isTrue();
    }

    @Test
    void shouldTimeoutWhenUpstreamIsDelayedBeyondThreshold() {
        wireMockServer.stubFor(get(urlEqualTo("/catalog/product/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(4000)
                        .withBody("{\"id\": 99, \"name\": \"Slow Product\"}")));

        HttpStatusCode status = getStatus("/product/slow");
        assertThat(status.is5xxServerError()).isTrue();
    }
}