package com.tcc.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.order.dto.OrderRequest;
import com.tcc.order.dto.OrderResponse;
import com.tcc.order.dto.UpdateStatusRequest;
import com.tcc.order.entity.OrderStatus;
import com.tcc.order.exception.InvalidStatusTransitionException;
import com.tcc.order.exception.OrderNotFoundException;
import com.tcc.order.security.AuthenticatedPrincipal;
import com.tcc.order.security.JwtService;
import com.tcc.order.security.SecurityConfig;
import com.tcc.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean OrderService orderService;
    @MockBean JwtService jwtService;

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 10, 0);

    private UsernamePasswordAuthenticationToken authAs(Long id, String email, String role) {
        var principal = new AuthenticatedPrincipal(id, email, role);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private OrderResponse sampleOrder(Long id, Long userId, OrderStatus status) {
        return new OrderResponse(id, userId, List.of(), new BigDecimal("500.00"), status, NOW, NOW);
    }

    @Test
    @DisplayName("POST /api/orders autenticado deve retornar 201 e repassar userId + token ao service")
    void create_Authenticated_ShouldReturnCreated() throws Exception {
        var request = new OrderRequest(List.of(new OrderRequest.OrderItemRequest(1L, 2)));
        var response = sampleOrder(10L, 5L, OrderStatus.PENDENTE);
        when(orderService.create(5L, request, "Bearer token-abc")).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .with(authentication(authAs(5L, "joao@test.com", "CUSTOMER")))
                        .header("Authorization", "Bearer token-abc")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.status", is("PENDENTE")));

        verify(orderService, times(1)).create(5L, request, "Bearer token-abc");
    }

    @Test
    @DisplayName("POST /api/orders sem autenticação deve ser rejeitado")
    void create_WithoutAuthentication_ShouldBeRejected() throws Exception {
        var request = new OrderRequest(List.of(new OrderRequest.OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer qualquer")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /api/orders com corpo inválido (sem itens) deve retornar 400")
    void create_WithEmptyItems_ShouldReturnBadRequest() throws Exception {
        var request = new OrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                        .with(authentication(authAs(5L, "joao@test.com", "CUSTOMER")))
                        .header("Authorization", "Bearer token-abc")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /api/orders/my-orders deve retornar os pedidos do usuário autenticado")
    void myOrders_ShouldReturnUserOrders() throws Exception {
        when(orderService.findByUser(5L)).thenReturn(List.of(sampleOrder(10L, 5L, OrderStatus.PENDENTE)));

        mockMvc.perform(get("/api/orders/my-orders")
                        .with(authentication(authAs(5L, "joao@test.com", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(5)));
    }

    @Test
    @DisplayName("GET /api/orders/{id} deve retornar 404 quando pedido não existe")
    void findById_ShouldReturnNotFoundWhenMissing() throws Exception {
        when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99")
                        .with(authentication(authAs(5L, "joao@test.com", "CUSTOMER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/orders com role ADMIN deve retornar todos os pedidos")
    @WithMockUser(roles = "ADMIN")
    void findAll_WithAdminRole_ShouldReturnAllOrders() throws Exception {
        when(orderService.findAll()).thenReturn(
                List.of(sampleOrder(1L, 5L, OrderStatus.PENDENTE), sampleOrder(2L, 6L, OrderStatus.CONFIRMADO)));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(orderService, never()).findByPeriod(any(), any());
    }

    @Test
    @DisplayName("GET /api/orders com startDate/endDate deve filtrar por período em vez de listar tudo")
    @WithMockUser(roles = "ADMIN")
    void findAll_WithDateRange_ShouldFilterByPeriod() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(orderService.findByPeriod(start, end)).thenReturn(List.of(sampleOrder(1L, 5L, OrderStatus.ENTREGUE)));

        mockMvc.perform(get("/api/orders").param("startDate", "2024-01-01").param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("ENTREGUE")));

        verify(orderService, times(1)).findByPeriod(start, end);
        verify(orderService, never()).findAll();
    }

    @Test
    @DisplayName("GET /api/orders sem role ADMIN deve retornar 403")
    @WithMockUser(roles = "CUSTOMER")
    void findAll_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status com role ADMIN deve atualizar o status")
    @WithMockUser(roles = "ADMIN")
    void updateStatus_WithAdminRole_ShouldReturnOk() throws Exception {
        var request = new UpdateStatusRequest(OrderStatus.CONFIRMADO);
        when(orderService.updateStatus(1L, request)).thenReturn(sampleOrder(1L, 5L, OrderStatus.CONFIRMADO));

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMADO")));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status deve retornar 422 para transição inválida")
    @WithMockUser(roles = "ADMIN")
    void updateStatus_WithInvalidTransition_ShouldReturnUnprocessableEntity() throws Exception {
        var request = new UpdateStatusRequest(OrderStatus.ENTREGUE);
        when(orderService.updateStatus(1L, request))
                .thenThrow(new InvalidStatusTransitionException(1L, OrderStatus.PENDENTE, OrderStatus.ENTREGUE));

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status sem role ADMIN deve retornar 403")
    @WithMockUser(roles = "CUSTOMER")
    void updateStatus_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        var request = new UpdateStatusRequest(OrderStatus.CONFIRMADO);

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel autenticado deve cancelar e repassar o token bruto")
    @WithMockUser(roles = "CUSTOMER")
    void cancel_Authenticated_ShouldReturnOk() throws Exception {
        when(orderService.cancel(1L, "Bearer token-abc")).thenReturn(sampleOrder(1L, 5L, OrderStatus.CANCELADO));

        mockMvc.perform(patch("/api/orders/1/cancel").header("Authorization", "Bearer token-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELADO")));

        verify(orderService, times(1)).cancel(1L, "Bearer token-abc");
    }
}
