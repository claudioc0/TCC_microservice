package com.tcc.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * O {@link RestClient.Builder} auto-configurado pelo Spring Boot usa, por padrão,
 * {@code SimpleClientHttpRequestFactory} (baseado em {@code HttpURLConnection} do JDK),
 * que não suporta o método HTTP PATCH — lança
 * {@code java.net.ProtocolException: Invalid HTTP method: PATCH}. Como o
 * {@link com.tcc.order.client.ProductClient} usa PATCH para ajustar estoque, trocamos
 * para {@link JdkClientHttpRequestFactory} (baseado em {@code java.net.http.HttpClient}),
 * que suporta PATCH nativamente.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory());
    }
}
