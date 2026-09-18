package com.example.orderservice.client;

import com.example.orderservice.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * No {@code url} attribute here on purpose: the plain {@code name} is resolved
 * through Eureka + Spring Cloud LoadBalancer to whatever host:port
 * payment-service actually registered with. That's what makes running
 * multiple payment-service instances (and load-balancing across them)
 * possible - a hardcoded url would defeat the point.
 */
@FeignClient(name = "payment-service")
public interface PaymentClient {

    @GetMapping("/payments/{id}")
    PaymentResponse getPayment(@PathVariable("id") Long id);
}
