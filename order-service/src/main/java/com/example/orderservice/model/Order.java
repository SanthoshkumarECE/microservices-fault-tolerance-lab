package com.example.orderservice.model;

/**
 * In-memory order record. A plain mutable class (not a record) since its
 * status/paymentStatus evolve after a payment attempt is made.
 */
public class Order {

    private final Long orderId;
    private final Long itemId;
    private final Double amount;
    private String status;
    private String paymentStatus;

    public Order(Long orderId, Long itemId, Double amount) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.amount = amount;
        this.status = "PENDING";
        this.paymentStatus = "PENDING";
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getItemId() {
        return itemId;
    }

    public Double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
