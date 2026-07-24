package com.tcc.order.client;

import com.tcc.order.client.dto.ProductDto;
import com.tcc.order.client.dto.StockAdjustmentPayload;
import com.tcc.order.exception.InsufficientStockException;
import com.tcc.order.exception.ProductNotFoundException;
import com.tcc.order.exception.ProductServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Único ponto de acesso do order-service ao catálogo/estoque de produtos.
 * Sempre via HTTP — nunca há acesso direto ao banco ou às classes do product-service.
 * O token do usuário original é repassado (pass-through) para preservar a identidade
 * da requisição também no product-service.
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient.Builder restClientBuilder,
                          @Value("${services.product-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public ProductDto getProduct(Long productId, String bearerToken) {
        try {
            return restClient.get()
                    .uri("/api/products/{id}", productId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (RestClientException e) {
            throw new ProductServiceUnavailableException(
                    "Falha ao consultar o produto " + productId + " no product-service.", e);
        }
    }

    public void decreaseStock(Long productId, int quantity, String bearerToken) {
        try {
            restClient.patch()
                    .uri("/api/products/{id}/stock/decrease", productId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(new StockAdjustmentPayload(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            throw new InsufficientStockException(productId);
        } catch (RestClientException e) {
            throw new ProductServiceUnavailableException(
                    "Falha ao reduzir o estoque do produto " + productId + " no product-service.", e);
        }
    }

    public void increaseStock(Long productId, int quantity, String bearerToken) {
        try {
            restClient.patch()
                    .uri("/api/products/{id}/stock/increase", productId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(new StockAdjustmentPayload(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (RestClientException e) {
            throw new ProductServiceUnavailableException(
                    "Falha ao devolver estoque do produto " + productId + " no product-service.", e);
        }
    }
}
