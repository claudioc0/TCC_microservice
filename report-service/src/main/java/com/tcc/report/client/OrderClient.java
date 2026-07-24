package com.tcc.report.client;

import com.tcc.report.client.dto.OrderSummaryDto;
import com.tcc.report.exception.OrderServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

/**
 * Único ponto de acesso do report-service aos pedidos.
 * Este serviço não tem banco próprio — toda a agregação parte dos dados obtidos aqui.
 */
@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(RestClient.Builder restClientBuilder,
                        @Value("${services.order-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public List<OrderSummaryDto> findByPeriod(LocalDate startDate, LocalDate endDate, String bearerToken) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/orders")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<OrderSummaryDto>>() {});
        } catch (RestClientException e) {
            throw new OrderServiceUnavailableException(
                    "Falha ao consultar pedidos no order-service para o período informado.", e);
        }
    }
}
