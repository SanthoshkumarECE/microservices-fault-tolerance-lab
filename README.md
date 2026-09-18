# Resilience Demo — Spring Boot Microservices with Resilience4j

A minimal microservices playground demonstrating service discovery, load balancing,
and resilience patterns (Retry, Circuit Breaker, Time Limiter) in a realistic
failure scenario: placing an order that depends on a flaky payment service.

## Stack

- Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0, Maven (multi-module)
- Netflix Eureka — service discovery
- Spring Cloud LoadBalancer + OpenFeign — client-side load balancing & HTTP calls
- Spring Cloud Gateway — single entry point for the frontend
- Resilience4j — Retry, Circuit Breaker, Time Limiter
- JUnit 5 + Mockito — unit tests
- Plain HTML/JS frontend (no build step)

## Services

| Service         | Port | Role                                                              |
|-----------------|------|--------------------------------------------------------------------|
| eureka-server   | 8761 | Service registry — every other service registers here             |
| payment-service | 8082 | Simulates a payment gateway; can be toggled to fail/delay          |
| order-service   | 8081 | Places orders; calls payment-service via Feign, wrapped in Resilience4j |
| api-gateway     | 8080 | Single entry point the frontend talks to; routes to order-service  |

The frontend calls `payment-service` directly (to toggle its failure mode) and
calls `order-service` through the `api-gateway` (to place orders).

## How it works

1. `order-service` validates the request and saves the order.
2. It then calls `payment-service` through `PaymentGatewayService`, a bean
   whose `createOrder(...)` method is wrapped with `@Retry` → `@CircuitBreaker`
   → `@TimeLimiter`.
3. If `payment-service` is slow, fails, or the circuit is open, the fallback
   method kicks in and the order is still created, marked
   `PAYMENT_PENDING_FALLBACK` instead of failing outright.

> **Why is the resilience logic in its own bean (`PaymentGatewayService`)
> instead of directly in `OrderService`?** Resilience4j's annotations are
> implemented via a Spring AOP proxy. A method can only be intercepted when
> it's called from *another* bean — an internal `this.method()` call bypasses
> the proxy and silently skips Retry/CircuitBreaker/TimeLimiter entirely. Keeping
> it on a separate bean, called through dependency injection, guarantees the
> proxy is actually used.

## Running it (IntelliJ, bundled Maven)

Build once from the root so all modules and their local dependencies resolve:

```bash
cd resilience-demo
mvn clean install
```

Then start the services **in this order** (each takes ~10-20s to be ready):

1. `eureka-server` (8761) — wait until it's up before starting anything else
2. `payment-service` (8082)
3. `order-service` (8081)
4. `api-gateway` (8080)

Check registration at `http://localhost:8761` — you should see all three
services listed once they've started.

Finally, open `frontend/index.html` directly in a browser (no server needed).

## Trying it out

- **Happy path**: place an order from the frontend → instant `CONFIRMED` response.
- **Failure path**: click "Toggle Delay" on the payment-service panel, then
  place an order → after a short timeout, you'll get a `CREATED` order with
  `PAYMENT_PENDING_FALLBACK` instead of an error.
- **Circuit breaker**: trigger several failures in a row and watch it open
  (fallback responses come back instantly); toggle the delay back off and
  after the wait duration it moves to half-open, then closed again.

## Observing resilience behavior

Actuator endpoints on `order-service` (`http://localhost:8081/actuator/...`):

- `/circuitbreakers`, `/retries` — current state/config
- `/circuitbreakerevents`, `/retryevents`, `/timelimiterevents` — a
  timestamped log of every retry attempt, state transition, and timeout

## Running tests

```bash
cd order-service
mvn test
```

## Project layout