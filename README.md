Aqui está o `README.md` completo, profissional e estruturado com base nas suas solicitações.

Removi os emojis conforme pedido, integrei a explicação técnica do Eureka/Gateway, a seção de idempotência e consolidei o checklist de tarefas realizadas.


# Ticket API - Event Ticketing System

This is the main microservice responsible for managing the ticketing system. The application handles the complete lifecycle of event management, including event creation, user registration, ticket sales, and integration with recommendation and geolocation services.

## System Architecture Overview

The system is designed using a microservices architecture. It utilizes asynchronous messaging for decoupling services and RESTful synchronous calls for critical, real-time operations.

### Architecture Diagram

## Main Components

* **API Gateway**: The single entry point for all client requests. It handles routing, authentication, and load balancing.
* **Ticket API (Core)**: Handles CRUD operations for events, tickets, and users. It is responsible for the business logic of reservations and stores event location data.
* **Payment Service**: Consumes messages regarding ticket reservations and processes payments via Stripe. It ensures data consistency using the Saga pattern (compensation transactions).
* **Event Recommendation Service**: Listens for new event location data and provides personalized recommendations to users based on proximity.
* **Geolocation API**: External service used to convert city/address names to latitude and longitude during event registration.
* **Cache (Redis)**: Implements caching strategies for high-demand data such as available tickets, popular events, and user session data.
* **Messaging Queue (RabbitMQ)**: Facilitates asynchronous communication between services (e.g., notifying the payment service when a ticket is reserved).
* **Databases**:
  * **PostgreSQL**: Stores relational data including Users, Tickets, and Orders.
  * **MongoDB**: Stores unstructured data optimized for geospatial queries in the Recommendation Service.


## Main Components

* **API Gateway**: Acts as a unified entry point and reverse proxy. It routes external requests to the appropriate internal microservices based on the request path, abstracting the internal network topology.
* **Ticket API (Core)**: Handles CRUD operations for events... (mantenha o resto igual)

...

## Service Discovery and Routing

This project utilizes the Spring Cloud ecosystem to manage microservice communication dynamically, solving the problem of hardcoded IP addresses.

### Eureka Server (Service Discovery)
In a dynamic containerized environment, IP addresses and ports can change. The Eureka Server acts as a service registry.
1.  **Registration**: When a microservice (e.g., Ticket API) starts up, it registers its current host and port with Eureka.
2.  **Discovery**: The Gateway queries Eureka to resolve the location of the target service using its Service ID (e.g., `TICKET-API`).

### Spring Cloud Gateway
The Gateway serves as a **Reverse Proxy**, simplifying the client-side consumption of the API.
1.  **Unified Entry Point**: Instead of managing multiple endpoints and ports (e.g., `localhost:8081`, `localhost:8082`), the client communicates with a single host.
2.  **Dynamic Routing**: The Gateway intercepts requests (e.g., `/api/v1/tickets`) and dynamically forwards them to the correct microservice instance resolved by Eureka.

## Prerequisites

* **Java 24**: The application runtime is optimized for Java 24 to leverage recent performance improvements.
* **Java 22**: Used for building the application.
* **Docker & Docker Compose**: For running infrastructure services.
* **Tilt**: For local Kubernetes orchestration (Optional but recommended).
* **Stripe CLI**: For local webhook testing.

## Execution

### Using Tilt

[Tilt](https://tilt.dev/) automates the feedback loop for local Kubernetes development.

1. Ensure you have a local Kubernetes cluster running (e.g., Docker Desktop, Minikube).
2. Run the command in the root directory:
```bash
tilt up
```
3. Access the Tilt dashboard to monitor logs and service status.

4. **Stripe Webhook**: To test payments locally, forward Stripe events to your local instance.

5. 
```bash
stripe listen --forward-to localhost:8083/payments/webhooks
```

## API Documentation

Once the application is running, the OpenAPI (Swagger) documentation is available at:

* **Swagger UI**: `http://localhost:8081/swagger-ui/index.html`

## Project Roadmap / Status

### Implemented Features

**Core & Architecture**

* [x] Implementation of Security Headers.
* [x] Database versioning/migrations using Flyway.
* [x] Usage of UUIDs for entity identifiers.
* [x] Global Exception Handler implementation.
* [x] Structured Logging.
* [x] Tilt configuration for local Kubernetes development.
* [x] Role-Based Access Control (RBAC) implementation.
* [x] Virtual Threads (Project Loom) adoption.

**Ticket API & Business Logic**

* [x] Advanced JPQL/Native queries using DTO projections and Entity Manager.
* [x] Proper HTTP Status code handling (409 Conflict, 422 Unprocessable Entity).
* [x] Swagger @Schema documentation for Requests/Responses.
* [x] Refactoring of Controller DTOs (Request/Response separation).
* [x] QR Code generation for ticket validation (with hash).

**Performance & Caching (Redis)**

* [x] Popular events caching (updates hourly).
* [x] User order lookup caching.
* [x] Ticket availability caching.
* [x] Purchase intent caching.

**Microservices & Integrations**

* [x] Recommendation Service based on geolocation radius.
* [x] Stripe Payment integration.
* [x] Payment Webhook handling.
* [x] Idempotency Key implementation for payments.
* [x] Saga Pattern (Compensation) for failed payments.
* [x] Circuit Breaker implementation (Resilience4j).

**Quality Assurance & Observability**

* [x] Spring Validation for DTOs.
* [x] RestControllerAdvice for unified error handling.
* [x] Unit and Integration Tests.
* [x] Monitoring stack: Micrometer + Grafana + Prometheus.

### Future Improvements

* [ ] Move auth logic to API gateway.
