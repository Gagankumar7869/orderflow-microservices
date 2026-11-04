# OrderFlow – Distributed Order & Inventory Management Platform

A microservices-based e-commerce order and inventory backend built with **Java 17**, **Spring Boot 3**, **Spring Security (JWT)**, **Spring Data JPA**, **Flyway**, **MySQL 8**, and **Apache Kafka**.

## Modules
* **`identity-service` (Port 8081):** User authentication, role management (`CUSTOMER`, `ADMIN`), and stateless JWT issuance.
* **`inventory-service` (Port 8082):** Product catalog, atomic stock reservation, expiry scheduler, and idempotent Kafka event consumer.
* **`order-service` (Port 8083):** Idempotent checkout orchestration, historical price snapshots, compensating rollback, and Transactional Outbox publisher.

See [`docs/architecture.md`](docs/architecture.md) for the complete system design and database ownership specification.
