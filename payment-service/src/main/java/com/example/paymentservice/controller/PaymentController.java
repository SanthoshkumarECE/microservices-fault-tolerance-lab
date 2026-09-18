package com.example.paymentservice.controller;

import com.example.paymentservice.model.PaymentResponse;
import com.example.paymentservice.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Minimal downstream payment endpoint. Deliberately has no service layer -
 * this microservice exists purely to be a fault-injection target for
 * ORDER-SERVICE's Resilience4j configuration.
 */
@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) throws InterruptedException {
        if (paymentRepository.isFailureModeEnabled()) {
            // Simulate a slow, failing downstream dependency: stall for 3s (longer than
            // ORDER-SERVICE's 2s TimeLimiter) and then blow up.
            Thread.sleep(3000);
            throw new RuntimeException("Simulated payment gateway outage");
        }
        return ResponseEntity.ok(new PaymentResponse(id, paymentRepository.getStatus(id)));
    }

    @PostMapping("/toggle-delay")
    public ResponseEntity<Map<String, Object>> toggleDelay() {
        boolean enabled = paymentRepository.toggleFailureMode();
        return ResponseEntity.ok(Map.of(
                "failureModeEnabled", enabled,
                "message", enabled
                        ? "Failure mode ON: /payments/{id} will now stall 3s then throw."
                        : "Failure mode OFF: /payments/{id} responds instantly."
        ));
    }
}
