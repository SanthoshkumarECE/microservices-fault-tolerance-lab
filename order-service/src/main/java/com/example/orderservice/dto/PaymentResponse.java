package com.example.orderservice.dto;

/**
 * Local mirror of PAYMENT-SERVICE's response contract, used by the Feign client.
 */
public record PaymentResponse(Long paymentId, String status) {
}
