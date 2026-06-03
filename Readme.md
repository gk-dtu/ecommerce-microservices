# Ecommerce Microservices

A production-style Spring Boot microservices backend with independent User, Product, and Order services featuring service discovery, API Gateway, circuit breakers, Docker containerization, PostgreSQL persistence, and JWT security.

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
- Docker + Docker Compose (containerization)
- Maven

## Services

| Service          | Port | Responsibility                                        |
|------------------|------|-------------------------------------------------------|
| discovery-server | 8761 | Eureka service registry                               |
| api-gateway      | 8080 | Single entry point, JWT validation, auth endpoints    |
| user-service     | 8081 | User registration and management                      |
| product-service  | 8082 | Product catalog and inventory                         |
| order-service    | 8083 | Order placement, validates user and product via Feign |

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
→ adds X-User-Name header → forwards to user-service ✅

INVALID/MISSING TOKEN:
→ 401 Unauthorized (missing/malformed)
→ 403 Forbidden (invalid/expired)
```

### Auth Endpoints (public — no token needed)

| Method | Endpoint         | Description                        |
|--------|------------------|------------------------------------|
| POST   | /auth/register   | Create account (returns 201)       |
| POST   | /auth/login      | Login and get JWT token            |
| GET    | /auth/validate   | Validate token (debug utility)     |

### Protected Endpoints (JWT required)

All `/users/**`, `/products/**`, `/orders/**` require:
```
Authorization: Bearer <your-jwt-token>
```

### Quick Test

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "aviraj", "password": "pass123"}'

# 2. Login — copy the token from response
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "aviraj", "password": "pass123"}'

# 3. Use token for protected routes
curl http://localhost:8080/users \
  -H "Authorization: Bearer <token>"

# 4. No token → 401
curl http://localhost:8080/users

# 5. Fake token → 403
curl http://localhost:8080/users \
  -H "Authorization: Bearer faketoken123"
```

### Security Implementation Details

```
JWT:
→ Algorithm: HS256 (HMAC + SHA-256)
→ Secret: externalized to environment variable (never in code)
→ Expiry: 24 hours (configurable via JWT_EXPIRATION)
→ Payload: username, issued-at, expiry

BCrypt:
→ Strength: 12 (industry recommended)
→ Salt: auto-generated, embedded in hash
→ Same password → different hash every time (rainbow tables useless)

Gateway Filter:
→ GlobalFilter runs at order -1 (first filter)
→ Public routes: /auth/**, /actuator/**
→ Protected routes: everything else
→ Identity propagation: X-User-Name header to downstream services

Database per Service:
→ authdb: user credentials (username + bcrypt hash + role)
→ Individual services never access authdb
```

---

## Running with Docker (Recommended)

### Prerequisites
- Docker Engine
- Docker Compose v2+

### Setup environment variables

```bash
# Copy example and fill in values
cp .env.example .env
```

`.env` file (gitignored — never committed):
```
JWT_SECRET=your-super-secret-key-minimum-32-characters
JWT_EXPIRATION=86400000
POSTGRES_USER=your-db-username
POSTGRES_PASSWORD=your-db-password
POSTGRES_DB=postgres
```

### Start all 7 containers with one command

```bash
docker compose up --build
```

Docker Compose handles everything automatically:
- Starts PostgreSQL first and waits for it to be healthy
- Creates 4 separate databases (userdb, productdb, orderdb, authdb)
- Starts Eureka discovery-server next
- Starts remaining 4 services only after Eureka is ready
- All secrets injected via environment variables — zero hardcoded credentials

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
| POST http://localhost:8080/auth/register | 201 Created |
| POST http://localhost:8080/auth/login | JWT token |

### Inspect databases directly

```bash
# Auth database
docker exec -it postgres psql -U $POSTGRES_USER -d authdb
\dt                          -- shows user_credentials table
SELECT username, role FROM user_credentials;
\q

# User database
docker exec -it postgres psql -U $POSTGRES_USER -d userdb
SELECT * FROM users;
\q
```

### Useful Docker commands

```bash
# Run in background
docker compose up --build -d

# View logs of specific service
docker compose logs -f api-gateway

# Restart single service
docker compose restart api-gateway

# Stop all containers (data preserved)
docker compose down

# Full reset including data
docker compose down -v

# Clean rebuild
docker compose down && docker compose up --build
```

---

## Running Locally (Without Docker)

Uses H2 in-memory database — no PostgreSQL setup needed.

```bash
# 1. Eureka Server
cd discovery-server && mvn spring-boot:run

# 2. Business services
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd product-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd order-service && mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. API Gateway (set env vars first)
export JWT_SECRET=any-local-secret-key-minimum-32-chars
cd api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Spring Profiles

Every service has 3 property files:

| File | Active when | Database |
|------|-------------|----------|
| `application.properties` | Always | Common config (port, eureka, resilience4j, jwt) |
| `application-local.properties` | `local` profile | H2 in-memory |
| `application-docker.properties` | `docker` profile | PostgreSQL |

Profile activated via docker-compose.yml:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

This follows the **12-factor app** principle — same codebase, config varies per environment.

---

## API Endpoints

All requests go through the API Gateway at `http://localhost:8080`

### Auth (public)
| Method | Endpoint          | Description              | Auth Required |
|--------|-------------------|--------------------------|---------------|
| POST   | /auth/register    | Create account           | No            |
| POST   | /auth/login       | Login, get JWT token     | No            |
| GET    | /auth/validate    | Validate token           | No            |

### User Service
| Method | Endpoint      | Description    | Auth Required |
|--------|---------------|----------------|---------------|
| POST   | /users        | Create user    | Yes           |
| PUT    | /users/{id}   | Update user    | Yes           |
| GET    | /users/{id}   | Get user by ID | Yes           |
| GET    | /users        | Get all users  | Yes           |

### Product Service
| Method | Endpoint         | Description       | Auth Required |
|--------|------------------|-------------------|---------------|
| POST   | /products        | Create product    | Yes           |
| GET    | /products/{id}   | Get product by ID | Yes           |
| GET    | /products        | Get all products  | Yes           |

### Order Service
| Method | Endpoint       | Description                               | Auth Required |
|--------|----------------|-------------------------------------------|---------------|
| POST   | /orders        | Place order (validates user and product)  | Yes           |
| GET    | /orders/{id}   | Get order by ID                           | Yes           |
| GET    | /orders        | Get all orders                            | Yes           |

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
    │  JwtAuthFilter   │             └──────────┬─────────┘
    │  AuthController  │                        │ Feign + Resilience4j
    └─────────┬────────┘              ┌─────────┴────────┐
              │                       │                  │
    ┌─────────┼─────────┐        user-service      product-service
    │         │         │           :8081              :8082
/users/**  /products/** /orders/**
```

### Docker + Database Architecture

```
Host Machine
│
└── ecommerce-net (bridge network)
    │
    ├── postgres :5432
    │     ├── authdb     (owned by api-gateway — credentials)
    │     ├── userdb     (owned by user-service)
    │     ├── productdb  (owned by product-service)
    │     └── orderdb    (owned by order-service)
    │
    ├── discovery-server :8761
    ├── api-gateway      :8080  ──→ postgres:5432/authdb
    ├── user-service     :8081  ──→ postgres:5432/userdb
    ├── product-service  :8082  ──→ postgres:5432/productdb
    └── order-service    :8083  ──→ postgres:5432/orderdb

Containers communicate by name, not IP.
All secrets via environment variables — zero hardcoded credentials.
Data stored in Docker named volume: postgres-data
```

---

## Docker Implementation Details

### Multi-stage Dockerfile (per service)

```
Stage 1 (builder) — maven:3.9.6-eclipse-temurin-17
  ├── Copies pom.xml first (layer cache — deps cached separately)
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
  │     └── pg_isready check every 10s
  │           init-db.sh creates authdb, userdb, productdb, orderdb
  │
  ├── Starts discovery-server (after postgres healthy)
  │     └── /actuator/health check every 15s
  │
  └── Starts all 4 services (after Eureka healthy)
        api-gateway, user-service, product-service, order-service
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
🚧 Unit + Integration Tests  
🚧 Kafka (async order events)  
🚧 Distributed Tracing (Zipkin)  
🚧 CI/CD (GitHub Actions)
