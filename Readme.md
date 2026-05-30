# Ecommerce Microservices

A production-style Spring Boot microservices backend with independent User, Product, and Order services featuring service discovery, API Gateway, circuit breakers, and full Docker containerization.

## Tech Stack

- Java 17, Spring Boot 3.2.12
- Spring Cloud OpenFeign (inter-service communication)
- Spring Cloud Netflix Eureka (service discovery)
- Spring Cloud Gateway (API gateway)
- Resilience4j (circuit breaker, retry, timeout)
- Spring Data JPA + Hibernate
- H2 In-memory Database
- Spring Boot Actuator (health checks)
- Docker + Docker Compose (containerization)
- Maven

## Services

| Service          | Port | Responsibility                                        |
|------------------|------|-------------------------------------------------------|
| discovery-server | 8761 | Eureka service registry                               |
| api-gateway      | 8080 | Single entry point, routes to all services            |
| user-service     | 8081 | User registration and management                      |
| product-service  | 8082 | Product catalog and inventory                         |
| order-service    | 8083 | Order placement, validates user and product via Feign |

---

## Running with Docker (Recommended)

### Prerequisites
- Docker Engine
- Docker Compose v2+

### Start all 5 services with one command

```bash
docker compose up --build
```

Docker Compose handles everything automatically:
- Builds all 5 service images using multi-stage builds
- Starts discovery-server first and waits for it to be healthy
- Starts remaining 4 services only after Eureka is ready
- Connects all services on a private bridge network

### Verify everything is running

| URL | What you should see |
|-----|---------------------|
| http://localhost:8761 | Eureka dashboard — all 4 services registered |
| http://localhost:8081/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/users | Response from user-service via gateway |

### Useful Docker commands

```bash
# Run in background (detached mode)
docker compose up --build -d

# View logs of a specific service
docker compose logs -f order-service

# Restart a single service
docker compose restart user-service

# Stop all containers
docker compose down

# Full clean rebuild (when something is broken)
docker compose down && docker compose up --build
```

---

## Running Locally (Without Docker)

Start services in this order:

```bash
# 1. Eureka Server must start first
cd discovery-server && mvn spring-boot:run

# 2. Start user and product services
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run

# 3. Start order service
cd order-service && mvn spring-boot:run

# 4. Start API Gateway last
cd api-gateway && mvn spring-boot:run
```

---

## API Endpoints

All requests go through the API Gateway at `http://localhost:8080`

### User Service
| Method | Endpoint      | Description    |
|--------|---------------|----------------|
| POST   | /users        | Create user    |
| PUT    | /users/{id}   | Update user    |
| GET    | /users/{id}   | Get user by ID |
| GET    | /users        | Get all users  |

### Product Service
| Method | Endpoint         | Description       |
|--------|------------------|-------------------|
| POST   | /products        | Create product    |
| GET    | /products/{id}   | Get product by ID |
| GET    | /products        | Get all products  |

### Order Service
| Method | Endpoint       | Description                               |
|--------|----------------|-------------------------------------------|
| POST   | /orders        | Place order (validates user and product)  |
| GET    | /orders/{id}   | Get order by ID                           |
| GET    | /orders        | Get all orders                            |

---

## Architecture

```
                    ┌─────────────────────┐
                    │   discovery-server  │
                    │      :8761          │
                    └──────────┬──────────┘
                               │ (all services register here)
                               │
              ┌────────────────┴────────────────┐
              │                                 │
    ┌─────────┴────────┐             ┌──────────┴─────────┐
    │   api-gateway    │             │    order-service   │
    │     :8080        │             │      :8083         │
    └─────────┬────────┘             └──────────┬─────────┘
              │                                 │ Feign + Resilience4j
    ┌─────────┼─────────┐              ┌────────┴────────┐
    │         │         │              │                 │
/users/**  /products/** /orders/**  user-service   product-service
    │         │         │            :8081            :8082
    │         │         │
user-     product-   order-
service   service    service
:8081     :8082      :8083
```

### Docker Network Architecture

```
Host Machine
│
└── ecommerce-net (bridge network)
    ├── discovery-server  (container name = hostname)
    ├── api-gateway       (resolves discovery-server:8761)
    ├── user-service      (resolves discovery-server:8761)
    ├── product-service   (resolves discovery-server:8761)
    └── order-service     (resolves discovery-server:8761)

Containers communicate by name, not IP.
Only gateway (:8080) and Eureka (:8761) exposed to host.
```

---

## Docker Implementation Details

### Multi-stage Dockerfile (per service)

```
Stage 1 (builder) — maven:3.9.6-eclipse-temurin-17
  ├── Copies pom.xml first (layer cache for dependencies)
  ├── Downloads all Maven dependencies
  └── Builds jar with mvn package -DskipTests

Stage 2 (runtime) — eclipse-temurin:17-jre-alpine
  ├── Minimal JRE-only image (~120MB vs ~500MB)
  ├── Non-root user for security
  └── Copies only the jar from Stage 1
```

### Health Check + Startup Ordering

```
docker compose up
  │
  ├── Starts discovery-server
  │     └── Polls /actuator/health every 15s
  │           Waits for {"status":"UP"}
  │
  └── Only then starts all 4 remaining services
        (guaranteed Eureka is ready before registration)
```

---

## Resilience Pattern in Order Service

- **Circuit Breaker** — opens after 50% failure rate in 5 calls, recovers after 10s
- **Retry** — 3 attempts with 2s wait, ignores 404s
- **Timeout** — 3s connect timeout, 5s read timeout via Feign config
- **Fallback** — returns 503 if service down, 404 if resource not found
- **Smart exception handling** — FeignException.NotFound → 404, all others → 503

---

## Current Progress

✅ Independent microservices (User, Product, Order)  
✅ REST APIs with validation and pagination  
✅ Standardized exception handling across all services  
✅ Consistent ApiResponse wrapper for all endpoints  
✅ Service-to-service communication via OpenFeign  
✅ Eureka Service Discovery  
✅ API Gateway (Spring Cloud Gateway)  
✅ Resilience4j (Circuit Breaker + Retry + Timeout + Fallback)  
✅ Docker containerization (multi-stage builds, health checks)  
✅ Docker Compose orchestration (startup ordering, bridge network)  
🚧 PostgreSQL (replacing H2)  
🚧 JWT Security at Gateway level  
🚧 Unit + Integration Tests  
🚧 Kafka (async order events)  
🚧 Distributed Tracing (Zipkin)  
🚧 CI/CD (GitHub Actions)
