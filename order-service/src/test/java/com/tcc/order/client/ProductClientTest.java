package com.tcc.order.client;

import com.tcc.order.client.dto.ProductDto;
import com.tcc.order.exception.InsufficientStockException;
import com.tcc.order.exception.ProductNotFoundException;
import com.tcc.order.exception.ProductServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductClientTest {

    private static final String BASE_URL = "http://product-service";
    private static final String TOKEN = "Bearer test-token";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ProductClient productClient = new ProductClient(builder, BASE_URL);

    @Test
    @DisplayName("getProduct deve retornar o produto e repassar o header Authorization")
    void getProductShouldReturnProductAndForwardAuthHeader() {
        server.expect(requestTo(BASE_URL + "/api/products/1"))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        {"id":1,"name":"Mouse","price":150.00,"stockQuantity":5}""", MediaType.APPLICATION_JSON));

        ProductDto product = productClient.getProduct(1L, TOKEN);

        assertThat(product.id()).isEqualTo(1L);
        assertThat(product.name()).isEqualTo("Mouse");
        server.verify();
    }

    @Test
    @DisplayName("getProduct deve lançar ProductNotFoundException para 404")
    void getProductShouldThrowNotFoundOn404() {
        server.expect(requestTo(BASE_URL + "/api/products/99"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> productClient.getProduct(99L, TOKEN))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("getProduct deve lançar ProductServiceUnavailableException para erro 500")
    void getProductShouldThrowUnavailableOn500() {
        server.expect(requestTo(BASE_URL + "/api/products/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> productClient.getProduct(1L, TOKEN))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    @DisplayName("decreaseStock deve enviar PATCH com a quantidade e o header Authorization")
    void decreaseStockShouldSendPatchWithQuantity() {
        server.expect(requestTo(BASE_URL + "/api/products/1/stock/decrease"))
                .andExpect(method(org.springframework.http.HttpMethod.PATCH))
                .andExpect(header("Authorization", TOKEN))
                .andExpect(content().json("{\"quantity\":2}"))
                .andRespond(withSuccess());

        productClient.decreaseStock(1L, 2, TOKEN);

        server.verify();
    }

    @Test
    @DisplayName("decreaseStock deve lançar InsufficientStockException para 422")
    void decreaseStockShouldThrowInsufficientStockOn422() {
        server.expect(requestTo(BASE_URL + "/api/products/1/stock/decrease"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> productClient.decreaseStock(1L, 100, TOKEN))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("increaseStock deve enviar PATCH com a quantidade")
    void increaseStockShouldSendPatchWithQuantity() {
        server.expect(requestTo(BASE_URL + "/api/products/1/stock/increase"))
                .andExpect(method(org.springframework.http.HttpMethod.PATCH))
                .andExpect(content().json("{\"quantity\":3}"))
                .andRespond(withSuccess());

        productClient.increaseStock(1L, 3, TOKEN);

        server.verify();
    }
}
