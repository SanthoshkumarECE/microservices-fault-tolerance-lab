package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.exception.InvalidOrderException;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGatewayService paymentGatewayService;

    public OrderService(OrderRepository orderRepository, PaymentGatewayService paymentGatewayService) {
        this.orderRepository = orderRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    /**
     * Public entry point used by the controller. The order record is created
     * first (so it exists in the repository regardless of what happens next),
     * then payment confirmation is attempted through {@link PaymentGatewayService},
     * a genuinely separate bean so its @Retry/@CircuitBreaker/@TimeLimiter
     * annotations actually get applied (see PaymentGatewayService's Javadoc
     * for why that separation matters).
     */
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        if (orderRequest.itemId() == null) {
            throw new InvalidOrderException("itemId is required");
        }
        if (orderRequest.amount() == null || orderRequest.amount() <= 0) {
            throw new InvalidOrderException("amount must be greater than zero");
        }

        Order order = orderRepository.save(orderRequest.itemId(), orderRequest.amount());
        return paymentGatewayService.createOrder(order).join();
    }
}