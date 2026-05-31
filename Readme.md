# Ecommerce Microservices

A production-style Spring Boot microservices backend with independent User, Product, and Order services featuring service discovery, API Gateway, circuit breakers, Docker containerization, and PostgreSQL persistence.

## Tech Stack

- Java 17, Spring Boot 3.2.12
- Spring Cloud OpenFeign (inter-service communication)
- Spring Cloud Netflix Eureka (service discovery)
- Spring Cloud Gateway (API gateway)
- Resilience4j (circuit breaker, retry, timeout)
- Spring Data JPA + Hibernate
- PostgreSQL 16 (production database)
- H2 In-memory Database (local development only)
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

### Start all 6 containers with one command

```bash
docker compose up --build
```

Docker Compose handles everything automatically:
- Starts PostgreSQL first and waits for it to be healthy
- Creates 3 separate databases (userdb, productdb, orderdb)
- Starts Eureka discovery-server next and waits for it to be healthy
- Starts remaining 4 services only after Eureka is ready
- Connects all containers on a private bridge network (ecommerce-net)

### Startup order

```
postgres → discovery-server → api-gateway
                            → user-service
                            → product-service
                            → order-service
```

### Verify everything is running

| URL | What you should see |
|-----|---------------------|
| http://localhost:8761 | Eureka dashboard — all 4 services registered |
| http://localhost:8081/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/users | Response from user-service via gateway |

### Test data persistence (proof PostgreSQL is working)

```bash
# Create a user
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Aviraj", "email": "aviraj@gmail.com"}'

# Stop all containers — data should survive this
docker compose down

# Start again (no --build needed)
docker compose up

# Data is still there
curl http://localhost:8080/users
```

With H2, data would be gone. With PostgreSQL + Docker volume, data persists forever.

### Inspect database directly

```bash
# Open PostgreSQL shell
docker exec -it postgres psql -U aviraj -d userdb

# Inside psql
\dt                  -- list all tables
SELECT * FROM users; -- see your data
\l                   -- list all databases
\q                   -- quit
```

### Useful Docker commands

```bash
# Run in background (detached mode)
docker compose up --build -d

# View logs of a specific service
docker compose logs -f order-service

# Restart a single service
docker compose restart user-service

# Stop all containers (data preserved in volume)
docker compose down

# Stop and delete all data (full reset)
docker compose down -v

# Full clean rebuild
docker compose down && docker compose up --build
```

---

## Running Locally (Without Docker)

Uses H2 in-memory database — no PostgreSQL setup needed.

```bash
# 1. Eureka Server must start first
cd discovery-server && mvn spring-boot:run

# 2. Start user and product services
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd product-service && mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. Start order service
cd order-service && mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Start API Gateway last
cd api-gateway && mvn spring-boot:run
```

---

## Spring Profiles

Each data service has 3 property files:

| File | Active when | Database |
|------|-------------|----------|
| `application.properties` | Always | Common config (port, eureka, resilience4j) |
| `application-local.properties` | `local` profile | H2 in-memory |
| `application-docker.properties` | `docker` profile | PostgreSQL |

Profile is set via environment variable in docker-compose.yml:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

This is the 12-factor app principle — same codebase, different config per environment.

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

### Docker + Database Architecture

```
Host Machine
│
└── ecommerce-net (bridge network)
    │
    ├── postgres :5432
    │     ├── userdb     (owned by user-service)
    │     ├── productdb  (owned by product-service)
    │     └── orderdb    (owned by order-service)
    │
    ├── discovery-server :8761
    ├── api-gateway      :8080
    ├── user-service     :8081  ──→ postgres:5432/userdb
    ├── product-service  :8082  ──→ postgres:5432/productdb
    └── order-service    :8083  ──→ postgres:5432/orderdb

Containers communicate by name, not IP.
Data stored in Docker named volume: postgres-data
Volume survives docker compose down — data never lost.
```

---

## Docker Implementation Details

### Multi-stage Dockerfile (per service)

```
Stage 1 (builder) — maven:3.9.6-eclipse-temurin-17
  ├── Copies pom.xml first (layer cache — deps not re-downloaded unless pom changes)
  ├── Downloads all Maven dependencies
  └── Builds jar with mvn package -DskipTests

Stage 2 (runtime) — eclipse-temurin:17-jre-alpine
  ├── Minimal JRE-only image (~120MB vs ~500MB)
  ├── Non-root user for container security
  └── Copies only the jar from Stage 1
```

### Health Check + Startup Ordering

```
docker compose up
  │
  ├── Starts postgres
  │     └── Polls pg_isready every 10s
  │           init-db.sh creates userdb, productdb, orderdb
  │
  ├── Starts discovery-server (after postgres healthy)
  │     └── Polls /actuator/health every 15s
  │
  └── Starts all 4 services (after Eureka healthy)
        Guaranteed correct startup order every time
```

### Database per Service Pattern

Each microservice owns its own database — a core microservices principle:

```
user-service    → userdb     (no other service touches this)
product-service → productdb  (no other service touches this)
order-service   → orderdb    (no other service touches this)

Services share data only via REST APIs — never via shared DB
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
✅ PostgreSQL migration (Spring profiles, Database per Service, volume persistence)  
🚧 JWT Security at Gateway level  
🚧 Unit + Integration Tests  
🚧 Kafka (async order events)  
🚧 Distributed Tracing (Zipkin)  
🚧 CI/CD (GitHub Actions)  
