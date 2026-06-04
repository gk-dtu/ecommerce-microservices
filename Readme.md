# Ecommerce Microservices

A production-style Spring Boot microservices backend with independent User, Product, and Order services featuring service discovery, API Gateway, circuit breakers, Docker containerization, PostgreSQL persistence, JWT security, and Kafka async event streaming.

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
- Spring Security (WebFlux reactive)
- JWT (JSON Web Token) — HS256 via JJWT 0.12.3
- BCrypt (password hashing, strength 12)
- Apache Kafka 3.6.2 (async event streaming, KRaft mode)
- Docker + Docker Compose (containerization)
- Maven

## Services

| Service          | Port | Responsibility                                                |
|------------------|------|---------------------------------------------------------------|
| discovery-server | 8761 | Eureka service registry                                       |
| api-gateway      | 8080 | Single entry point, JWT validation, auth endpoints            |
| user-service     | 8081 | User registration and management                              |
| product-service  | 8082 | Product catalog and inventory                                 |
| order-service    | 8083 | Order placement, validates user/product, publishes Kafka event|

---

## Event-Driven Architecture (Kafka)

### Flow

```
POST /orders
     ↓
order-service validates user + product (Feign)
     ↓
saves order to orderdb (PostgreSQL)
     ↓
publishes OrderPlacedEvent to Kafka topic "order.placed"
     ↓ (async — different thread)
OrderEventConsumer processes event
     ↓
future: notification-service, inventory-service consume same event
        without any change to order-service
```

### Why async after DB save?

```
WRONG order:
1. publish event → 2. save to DB (if DB fails → event exists but no order = inconsistency)

CORRECT order (what we do):
1. save to DB ✅ → 2. publish event
If Kafka fails → order still saved, error logged, retry possible
```

### OrderPlacedEvent payload

```json
{
  "eventId": "uuid-unique-per-event",
  "eventTime": "2026-06-04T11:20:45",
  "orderId": 3,
  "userId": 2,
  "productId": 2,
  "quantity": 10,
  "totalAmount": 1005000.0,
  "status": "PLACED",
  "placedBy": "aviraj"
}
```

### Kafka Setup (KRaft mode — no Zookeeper)

```
Topic:      order.placed
Partitions: 3 (parallel consumption ready)
Replicas:   1 (single broker setup)
Mode:       KRaft (Zookeeper-free, Kafka 3.3+)

Producer thread: [nio-8083-exec-1]  → HTTP request thread
Consumer thread: [ntainer#0-0-C-1]  → separate async thread
Truly decoupled — producer never waits for consumer
```

### Verify Kafka topics

```bash
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

### userId vs placedBy

```
userId   = business entity (whose order) — from request body
placedBy = authenticated identity (who is logged in) — from JWT X-User-Name header

Used for audit trail and fraud detection:
Same userId, different placedBy → suspicious activity 🚨
```

---

## Security

### Authentication Flow

```
REGISTER:
POST /auth/register → gateway → BCrypt hash → save to authdb → 201 Created

LOGIN:
POST /auth/login → gateway → BCrypt verify → generate JWT → return token

PROTECTED REQUEST:
GET /users
Authorization: Bearer <token>
→ JwtAuthFilter validates token → extracts username
→ adds X-User-Name header → forwards to service ✅

INVALID/MISSING TOKEN:
→ 401 Unauthorized (missing/malformed)
→ 403 Forbidden (invalid/expired)
```

### Auth Endpoints (public — no token needed)

| Method | Endpoint       | Description                  |
|--------|----------------|------------------------------|
| POST   | /auth/register | Create account (returns 201) |
| POST   | /auth/login    | Login and get JWT token      |
| GET    | /auth/validate | Validate token (debug)       |

### Protected Endpoints (JWT required)

All `/users/**`, `/products/**`, `/orders/**` require:
```
Authorization: Bearer <your-jwt-token>
```

### Quick Auth Test

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "aviraj", "password": "pass123"}'

# 2. Login — copy token from response
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "aviraj", "password": "pass123"}'

# 3. Place order — triggers Kafka event
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"userId": 1, "productId": 1, "quantity": 2}'

# 4. Watch Kafka event in logs
docker compose logs -f order-service
```

---

## Running with Docker (Recommended)

### Prerequisites
- Docker Engine
- Docker Compose v2+

### Setup environment variables

```bash
cp .env.example .env
# fill in values
```

`.env` file (gitignored):
```
JWT_SECRET=your-super-secret-key-minimum-32-characters
JWT_EXPIRATION=86400000
POSTGRES_USER=your-db-username
POSTGRES_PASSWORD=your-db-password
POSTGRES_DB=postgres
```

### Start all 8 containers with one command

```bash
docker compose up --build
```

Containers started:
- postgres (PostgreSQL 16)
- kafka (KRaft mode, no Zookeeper)
- discovery-server (Eureka)
- api-gateway
- user-service
- product-service
- order-service

### Startup order

```
postgres + kafka → discovery-server → api-gateway
                                    → user-service
                                    → product-service
                                    → order-service (depends on kafka too)
```

### Verify everything is running

| URL | What you should see |
|-----|---------------------|
| http://localhost:8761 | Eureka — all 4 services registered |
| http://localhost:8081/actuator/health | `{"status":"UP"}` |
| POST http://localhost:8080/auth/register | 201 Created |
| POST http://localhost:8080/auth/login | JWT token |
| POST http://localhost:8080/orders + token | Order created + Kafka event in logs |

### Inspect databases directly

```bash
# Auth database
docker exec -it postgres psql -U $POSTGRES_USER -d authdb
SELECT username, role FROM user_credentials;
\q

# Order database
docker exec -it postgres psql -U $POSTGRES_USER -d orderdb
SELECT * FROM orders;
\q
```

### Useful Docker commands

```bash
# Run in background
docker compose up --build -d

# Watch Kafka events
docker compose logs -f order-service

# View logs of specific service
docker compose logs -f api-gateway

# Stop all (data preserved)
docker compose down

# Full reset including data
docker compose down -v

# Clean rebuild
docker compose down && docker compose up --build
```

---

## Running Locally (Without Docker)

Uses H2 in-memory database. Kafka not available locally — only in Docker.

```bash
# 1. Eureka Server
cd discovery-server && mvn spring-boot:run

# 2. Business services
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd product-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd order-service && mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. API Gateway
export JWT_SECRET=any-local-secret-key-minimum-32-chars
cd api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Spring Profiles

Every service has 3 property files:

| File | Active when | Database | Kafka |
|------|-------------|----------|-------|
| `application.properties` | Always | Common config | None |
| `application-local.properties` | `local` profile | H2 in-memory | Disabled |
| `application-docker.properties` | `docker` profile | PostgreSQL | kafka:9092 |

Kafka config exists ONLY in `application-docker.properties` — Spring does not auto-configure Kafka when bootstrap-servers is absent. Local profile runs cleanly without Kafka.

---

## API Endpoints

All requests go through API Gateway at `http://localhost:8080`

### Auth (public)
| Method | Endpoint        | Description          | Auth Required |
|--------|-----------------|----------------------|---------------|
| POST   | /auth/register  | Create account       | No            |
| POST   | /auth/login     | Login, get JWT token | No            |
| GET    | /auth/validate  | Validate token       | No            |

### User Service
| Method | Endpoint      | Description    | Auth Required |
|--------|---------------|----------------|---------------|
| POST   | /users        | Create user    | Yes           |
| PUT    | /users/{id}   | Update user    | Yes           |
| GET    | /users/{id}   | Get user by ID | Yes           |
| GET    | /users        | Get all users  | Yes           |

### Product Service
| Method | Endpoint        | Description       | Auth Required |
|--------|-----------------|-------------------|---------------|
| POST   | /products       | Create product    | Yes           |
| GET    | /products/{id}  | Get product by ID | Yes           |
| GET    | /products       | Get all products  | Yes           |

### Order Service
| Method | Endpoint      | Description                              | Auth Required |
|--------|---------------|------------------------------------------|---------------|
| POST   | /orders       | Place order → publishes Kafka event      | Yes           |
| GET    | /orders/{id}  | Get order by ID                          | Yes           |
| GET    | /orders       | Get all orders                           | Yes           |

---

## Architecture

```
                    ┌─────────────────────┐
                    │   discovery-server  │
                    │      :8761          │
                    └──────────┬──────────┘
                               │
              ┌────────────────┴────────────────┐
              │                                 │
    ┌─────────┴────────┐             ┌──────────┴─────────┐
    │   api-gateway    │             │    order-service   │
    │     :8080        │             │      :8083         │
    │  JwtAuthFilter   │             └──────────┬─────────┘
    │  AuthController  │                        │ Feign + Resilience4j
    └─────────┬────────┘              ┌─────────┴────────┐
              │                       │                  │
    ┌─────────┼─────────┐        user-service      product-service
    │         │         │           :8081              :8082
/users/**  /products/** /orders/**
```

### Event-Driven Flow

```
order-service
     │
     ├──→ orderdb (PostgreSQL) — save order
     │
     └──→ kafka:9092
              │
              └──→ topic: order.placed (3 partitions)
                        │
                        └──→ OrderEventConsumer (async thread)
                                  │
                             future consumers:
                             notification-service
                             inventory-service
                             analytics-service
```

### Docker + Database Architecture

```
Host Machine
│
└── ecommerce-net (bridge network)
    │
    ├── postgres :5432
    │     ├── authdb     (api-gateway — credentials)
    │     ├── userdb     (user-service)
    │     ├── productdb  (product-service)
    │     └── orderdb    (order-service)
    │
    ├── kafka :9092 (KRaft, no Zookeeper)
    │     └── topic: order.placed (3 partitions)
    │
    ├── discovery-server :8761
    ├── api-gateway      :8080
    ├── user-service     :8081
    ├── product-service  :8082
    └── order-service    :8083

Volumes:
postgres-data → database persistence
kafka-data    → message persistence
```

---

## Docker Implementation Details

### Multi-stage Dockerfile (per service)

```
Stage 1 (builder) — maven:3.9.6-eclipse-temurin-17
  ├── Copies pom.xml first (layer cache)
  ├── Downloads Maven dependencies
  └── Builds jar with mvn package -DskipTests

Stage 2 (runtime) — eclipse-temurin:17-jre-alpine
  ├── ~120MB vs ~500MB single stage
  ├── Non-root user for security
  └── Copies only the jar
```

### Startup Ordering

```
postgres    → pg_isready healthcheck
kafka       → kafka-topics list healthcheck
     ↓
discovery-server → /actuator/health
     ↓
all 4 services start (guaranteed Eureka + DB + Kafka ready)
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
✅ JWT Security at Gateway level (HS256, GlobalFilter, public/protected routes)  
✅ BCrypt password hashing (strength 12, salt embedded)  
✅ DB-backed auth (register/login with authdb, zero hardcoded credentials)  
✅ Kafka async event streaming (KRaft mode, OrderPlacedEvent, producer/consumer)
🚧 Unit + Integration Tests  
🚧 Distributed Tracing (Zipkin)  
🚧 CI/CD (GitHub Actions)

