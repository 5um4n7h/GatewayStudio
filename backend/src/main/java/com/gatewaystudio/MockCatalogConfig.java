package com.gatewaystudio;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Configuration
public class MockCatalogConfig {

    @Bean
    public Object configureMockCatalog(WireMockServer wireMockServer) {

        wireMockServer.stubFor(
                get(urlPathMatching("/catalog/product/[0-9]+"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                            {
                                              "id": 123,
                                              "name": "Demo Product",
                                              "description": "Mock catalog product"
                                            }
                                            """)
                        )
        );

        return new Object();
    }
}
