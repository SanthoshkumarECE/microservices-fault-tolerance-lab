package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PaymentResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, no Resilience4j AOP proxy is loaded
 * here. These verify PaymentGatewayService's own logic - including calling
 * createOrderFallback() directly as a plain method - rather than the AOP
 * wiring itself, which only exists once Spring actually boots the
 * application and wraps this bean in a proxy.
 */
@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentGatewayService paymentGatewayService;

    @Test
    void createOrder_HappyPath_ReturnsConfirmedOrderWithPaymentStatus() {
        Order order = new Order(1001L, 55L, 250.0);
        when(paymentClient.getPayment(1001L)).thenReturn(new PaymentResponse(1001L, "SUCCESS"));
        when(orderRepository.update(order)).thenReturn(order);

        OrderResponse response = paymentGatewayService.createOrder(order).join();

        assertThat(response.orderId()).isEqualTo(1001L);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.paymentStatus()).isEqualTo("SUCCESS");
        verify(orderRepository).update(order);
    }

    @Test
    void createOrder_WhenPaymentClientThrows_FutureCompletesExceptionally() {
        Order order = new Order(3003L, 12L, 40.0);
        when(paymentClient.getPayment(3003L)).thenThrow(new RuntimeException("Simulated payment gateway outage"));

        CompletableFuture<OrderResponse> future = paymentGatewayService.createOrder(order);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
        verify(orderRepository, never()).update(any());
    }

    @Test
    void createOrderFallback_WhenPaymentServiceFails_ReturnsPendingFallbackStatus() {
        Order order = new Order(2002L, 77L, 99.5);
        when(orderRepository.update(order)).thenReturn(order);

        OrderResponse response = paymentGatewayService
                .createOrderFallback(order, new RuntimeException("Payment service down"))
                .join();

        assertThat(response.orderId()).isEqualTo(2002L);
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.paymentStatus()).isEqualTo("PAYMENT_PENDING_FALLBACK");
        verify(orderRepository).update(order);
    }
}