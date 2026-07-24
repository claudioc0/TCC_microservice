package com.tcc.report.client;

import com.tcc.report.exception.OrderServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrderClientTest {

    private static final String BASE_URL = "http://order-service";
    private static final String TOKEN = "Bearer admin-token";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OrderClient orderClient = new OrderClient(builder, BASE_URL);

    @Test
    @DisplayName("findByPeriod deve consultar /api/orders com startDate/endDate e repassar o token")
    void findByPeriodShouldQueryOrdersWithDateRangeAndToken() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        server.expect(requestTo(BASE_URL + "/api/orders?startDate=2024-01-01&endDate=2024-01-31"))
                .andExpect(header("Authorization", TOKEN))
                .andRespond(withSuccess("""
                        [{"id":1,"userId":10,"totalAmount":100.00,"status":"ENTREGUE","createdAt":"2024-01-15T10:00:00"}]
                        """, MediaType.APPLICATION_JSON));

        var orders = orderClient.findByPeriod(start, end, TOKEN);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).status()).isEqualTo("ENTREGUE");
        server.verify();
    }

    @Test
    @DisplayName("findByPeriod deve lançar OrderServiceUnavailableException quando o order-service falha")
    void findByPeriodShouldThrowWhenOrderServiceFails() {
        LocalDate today = LocalDate.now();
        server.expect(requestTo(BASE_URL + "/api/orders?startDate=" + today + "&endDate=" + today))
                .andRespond(withServerError());

        assertThatThrownBy(() -> orderClient.findByPeriod(today, today, TOKEN))
                .isInstanceOf(OrderServiceUnavailableException.class);
    }
}
