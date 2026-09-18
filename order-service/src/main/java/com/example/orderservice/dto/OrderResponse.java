package com.example.orderservice.dto;

public record OrderResponse(Long orderId, String status, String paymentStatus) {
}
