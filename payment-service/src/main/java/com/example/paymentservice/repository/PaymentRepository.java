package com.example.paymentservice.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory "database" for mock payment statuses. No external DB - just a
 * ConcurrentHashMap, as required for this practice project.
 */
@Repository
public class PaymentRepository {

    private final Map<Long, String> paymentStatusById = new ConcurrentHashMap<>();
    private final AtomicBoolean failureModeEnabled = new AtomicBoolean(false);

    public PaymentRepository() {
        // Seed a few mock payments so GET /payments/{id} has something realistic to return.
        paymentStatusById.put(1L, "SUCCESS");
        paymentStatusById.put(2L, "SUCCESS");
        paymentStatusById.put(3L, "FAILED");
    }

    public String getStatus(Long paymentId) {
        return paymentStatusById.getOrDefault(paymentId, "SUCCESS");
    }

    /**
     * Flips the failure/delay switch used to simulate an unhealthy downstream dependency.
     *
     * @return the new state (true = failure mode ON)
     */
    public boolean toggleFailureMode() {
        boolean newValue = !failureModeEnabled.get();
        failureModeEnabled.set(newValue);
        return newValue;
    }

    public boolean isFailureModeEnabled() {
        return failureModeEnabled.get();
    }
}
