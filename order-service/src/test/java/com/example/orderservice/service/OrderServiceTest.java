package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.exception.InvalidOrderException;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * OrderService only orchestrates: validate -> save -> delegate to
 * PaymentGatewayService. The resilience behavior itself (retry, circuit
 * breaker, timeout, fallback) belongs to PaymentGatewayService and is
 * tested in PaymentGatewayServiceTest - keeping the two separate here
 * mirrors why they're separate beans in the first place (see
 * PaymentGatewayService's Javadoc on the AOP self-invocation trap).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_HappyPath_DelegatesToPaymentGatewayServiceAndReturnsItsResult() {
        Order order = new Order(1001L, 55L, 250.0);
        when(orderRepository.save(55L, 250.0)).thenReturn(order);
        OrderResponse expected = new OrderResponse(1001L, "CONFIRMED", "SUCCESS");
        when(paymentGatewayService.createOrder(order)).thenReturn(CompletableFuture.completedFuture(expected));

        OrderResponse response = orderService.placeOrder(new OrderRequest(55L, 250.0));

        assertThat(response).isEqualTo(expected);
        verify(paymentGatewayService).createOrder(order);
    }

    @Test
    void placeOrder_NonPositiveAmount_ThrowsInvalidOrderExceptionWithoutCallingPaymentGateway() {
        assertThatThrownBy(() -> orderService.placeOrder(new OrderRequest(55L, -10.0)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("amount");

        verifyNoInteractions(paymentGatewayService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void placeOrder_MissingItemId_ThrowsInvalidOrderExceptionWithoutCallingPaymentGateway() {
        assertThatThrownBy(() -> orderService.placeOrder(new OrderRequest(null, 10.0)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("itemId");

        verifyNoInteractions(paymentGatewayService);
        verifyNoInteractions(orderRepository);
    }
}