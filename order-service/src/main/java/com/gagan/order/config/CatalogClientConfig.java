package com.gagan.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CatalogClientConfig {

    @Bean
    public RestClient catalogRestClient(
            @Value("${catalog.service.url}") String catalogServiceUrl,
            @Value("${catalog.service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${catalog.service.read-timeout-ms:5000}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(catalogServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
}