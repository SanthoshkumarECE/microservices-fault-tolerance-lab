package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.exception.InvalidOrderException;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test: only the web layer is loaded. OrderService is replaced with a
 * Mockito mock via Spring Boot 3.4's @MockitoBean, so this exercises routing,
 * request binding, JSON serialization, and @RestControllerAdvice mapping only.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrder_ValidRequest_ReturnsCreatedWithOrderResponse() throws Exception {
        OrderRequest request = new OrderRequest(10L, 199.99);
        OrderResponse response = new OrderResponse(5001L, "CONFIRMED", "SUCCESS");
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(5001))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    void placeOrder_PaymentServiceDown_ReturnsFallbackPaymentStatus() throws Exception {
        OrderRequest request = new OrderRequest(11L, 50.0);
        OrderResponse response = new OrderResponse(5002L, "CREATED", "PAYMENT_PENDING_FALLBACK");
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PAYMENT_PENDING_FALLBACK"));
    }

    @Test
    void placeOrder_InvalidAmount_ReturnsBadRequestViaExceptionHandler() throws Exception {
        OrderRequest request = new OrderRequest(12L, -5.0);
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new InvalidOrderException("amount must be greater than zero"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amount must be greater than zero"));
    }
}
