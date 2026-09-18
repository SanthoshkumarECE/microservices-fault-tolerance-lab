package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PaymentResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Deliberately its own bean, separate from OrderService.
 *
 * Resilience4j's @Retry / @CircuitBreaker / @TimeLimiter are implemented as
 * Spring AOP advice: Spring wraps this bean in a proxy that intercepts calls
 * to createOrder(...) from OUTSIDE this class. If this method instead lived
 * on OrderService and OrderService called it via a plain internal
 * `this.createOrder(...)` (self-invocation), that call would never pass
 * through the proxy - the annotations would be silently ignored at runtime,
 * with no error to indicate anything was wrong. Keeping this on a separate
 * bean, invoked from OrderService only through its injected reference,
 * guarantees the call goes through the proxy and the resilience logic
 * actually runs.
 */
@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);
    private static final String RESILIENCE_INSTANCE = "paymentService";

    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;

    public PaymentGatewayService(PaymentClient paymentClient, OrderRepository orderRepository) {
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
    }

    /**
     * Calls PAYMENT-SERVICE through Feign, guarded by Retry -> CircuitBreaker -> TimeLimiter.
     * Must return a CompletableFuture for @TimeLimiter to be able to enforce its timeout.
     */
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "createOrderFallback")
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "createOrderFallback")
    @TimeLimiter(name = RESILIENCE_INSTANCE, fallbackMethod = "createOrderFallback")
    public CompletableFuture<OrderResponse> createOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            PaymentResponse paymentResponse = paymentClient.getPayment(order.getOrderId());
            order.setStatus("CONFIRMED");
            order.setPaymentStatus(paymentResponse.status());
            orderRepository.update(order);
            return new OrderResponse(order.getOrderId(), order.getStatus(), order.getPaymentStatus());
        });
    }

    /**
     * Fallback for {@link #createOrder(Order)}. Its signature must mirror the target
     * method plus a trailing Throwable. Triggered on retry exhaustion, an open
     * circuit, or a TimeLimiter timeout.
     */
    public CompletableFuture<OrderResponse> createOrderFallback(Order order, Throwable throwable) {
        log.warn("PAYMENT-SERVICE unavailable for order {} ({}). Falling back.",
                order.getOrderId(), throwable.toString());
        order.setStatus("CREATED");
        order.setPaymentStatus("PAYMENT_PENDING_FALLBACK");
        orderRepository.update(order);
        return CompletableFuture.completedFuture(
                new OrderResponse(order.getOrderId(), order.getStatus(), order.getPaymentStatus()));
    }
}