package com.example.orderservice.repository;

import com.example.orderservice.model.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class OrderRepository {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1000);

    public Order save(Long itemId, Double amount) {
        Long orderId = idGenerator.incrementAndGet();
        Order order = new Order(orderId, itemId, amount);
        orders.put(orderId, order);
        return order;
    }

    public Order update(Order order) {
        orders.put(order.getOrderId(), order);
        return order;
    }

    public Optional<Order> findById(Long orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
