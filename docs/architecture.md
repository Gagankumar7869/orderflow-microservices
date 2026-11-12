# OrderFlow – System Architecture & Engineering Specification

## 1. Executive Summary
OrderFlow is a distributed e-commerce backend engineered to solve high-concurrency inventory reservation, idempotent checkout processing, and eventual consistency across microservices without distributed two-phase locking (2PC).

## 2. Microservices Topology & Database Ownership

| Service | Port | Database Schema | Owned Tables | Core Responsibility |
| :--- | :--- | :--- | :--- | :--- |
| `identity-service` | `8081` | `orderflow_identity_db` | `users` | Authentication, BCrypt password hashing, stateless JWT issuance (`CUSTOMER`, `ADMIN`). |
| `catalog-service` | `8082` | `orderflow_catalog_db` | `products`, `inventory`, `inventory_reservations`, `processed_events` | Product catalog, atomic conditional stock locking, reservation state machine, idempotent Kafka consumer. |
| `order-service` | `8083` | `orderflow_order_db` | `orders`, `order_items`, `outbox_events` | Payload-hashed idempotency, split-transaction checkout orchestration, REST compensation, Transactional Outbox publisher. |

**Database Isolation Rule:** Cross-service SQL foreign keys are strictly prohibited. Cross-domain references (`user_id`, `product_id`, `order_reference`) are enforced as logical identifiers validated via JWT claims or synchronous REST contracts.

## 3. Core Architectural Patterns

### 3.1 Atomic Stock Reservation & Reservation State Machine
To prevent overselling under concurrent requests without locking entire tables, `catalog-service` uses a single atomic conditional SQL update:
`UPDATE inventory SET available_quantity = available_quantity - :qty, reserved_quantity = reserved_quantity + :qty WHERE product_id = :pid AND available_quantity >= :qty`

Each item reservation follows a strict 4-state lifecycle (`ReservationStatus`):
* **`RESERVED`**: Stock temporarily held during synchronous checkout (`available` decreases, `reserved` increases).
* **`COMMITTED`**: Finalized asynchronously via Kafka `ORDER_CONFIRMED` event (`reserved` decreases, `available` unchanged).
* **`RELEASED`**: Restored via synchronous checkout compensation or `ORDER_CANCELLED` event (`reserved` decreases, `available` increases; or restocked if previously committed).
* **`EXPIRED`**: Automatically reclaimed by the background expiry scheduler if `expires_at < NOW()` while still in `RESERVED` status.

### 3.2 Split-Transaction Checkout & Compensating Rollback
Because a local `@Transactional` boundary in `order-service` cannot roll back a committed reservation in `catalog-service`, checkout is split into two distinct layers:
1. **`OrderOrchestrationService` (Non-Transactional):** Validates idempotency, invokes `catalog-service` via REST to reserve stock and retrieve authoritative unit prices, and delegates local persistence to `OrderPersistenceService`.
2. **`OrderPersistenceService` (`@Transactional`):** Atomically persists the `Order`, `OrderItem` historical price snapshots, and the `OutboxEvent` in `orderflow_order_db`.
3. **Compensation:** If `OrderPersistenceService` throws a persistence or transaction commit exception, `OrderOrchestrationService` catches it outside the transaction boundary and invokes the idempotent `/api/v1/inventory/reservations/release` endpoint.

### 3.3 Transactional Outbox & Sequential Kafka Event Processing
* **At-Least-Once Outbox Relay:** `order-service` writes `outbox_events` inside the same database transaction as the `Order` state change. A scheduled poller claims pending events (`FOR UPDATE SKIP LOCKED`), publishes them to the `order-events` Kafka topic using `orderReference` as the partition key, and marks them `PUBLISHED` only after broker acknowledgment.
* **Durable Consumer Deduplication:** `catalog-service` records every consumed `event_id` in `processed_events` within the exact same `@Transactional` boundary as the inventory state update, guaranteeing exactly-once stock mutation even if the Outbox publisher replays an event after a crash.
