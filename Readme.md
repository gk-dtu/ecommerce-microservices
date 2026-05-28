# Ecommerce Microservices

A production-style Spring Boot microservices backend with independent User, Product, and Order services featuring service discovery, API Gateway, and circuit breakers.

## Tech Stack

- Java 17, Spring Boot 3.2.12
- Spring Cloud OpenFeign (inter-service communication)
- Spring Cloud Netflix Eureka (service discovery)
- Spring Cloud Gateway (API gateway)
- Resilience4j (circuit breaker, retry, timeout)
- Spring Data JPA + Hibernate
- H2 In-memory Database
- Maven

## Services

| Service          | Port | Responsibility                                       |
|------------------|------|------------------------------------------------------|
| discovery-server | 8761 | Eureka service registry                              |
| api-gateway      | 8080 | Single entry point, routes to all services           |
| user-service     | 8081 | User registration and management                     |
| product-service  | 8082 | Product catalog and inventory                        |
| order-service    | 8083 | Order placement, validates user and product via Feign|

## How to Run

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

- Eureka Dashboard: http://localhost:8761
- All APIs via Gateway: http://localhost:8080

## API Endpoints

### User Service
| Method | Endpoint     | Description    |
|--------|--------------|----------------|
| POST   | /users       | Create user    |
| PUT    | /users/{id}  | Update user    |
| GET    | /users/{id}  | Get user by ID |
| GET    | /users       | Get all users  |

### Product Service
| Method | Endpoint        | Description       |
|--------|-----------------|-------------------|
| POST   | /products       | Create product    |
| GET    | /products/{id}  | Get product by ID |
| GET    | /products       | Get all products  |

### Order Service
| Method | Endpoint      | Description                              |
|--------|---------------|------------------------------------------|
| POST   | /orders       | Place order (validates user and product) |
| GET    | /orders/{id}  | Get order by ID                          |
| GET    | /orders       | Get all orders                           |

## Architecture

## Architecture

```
                    ┌─────────────────────┐
                    │   discovery-server  │
                    │      :8761          │
                    └──────────┬──────────┘
                               │ (all services register)
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
## Resilience Pattern in Order Service

- **Circuit Breaker** — opens after 50% failure rate in 5 calls, recovers after 10s
- **Retry** — 3 attempts with 2s wait, ignores 404s
- **Timeout** — 3s connect timeout, 5s read timeout via Feign config
- **Fallback** — returns 503 if service down, 404 if resource not found
- **Smart exception handling** — FeignException.NotFound → 404, all others → 503

## Current Progress

✅ Independent microservices (User, Product, Order)  
✅ REST APIs with validation and pagination  
✅ Standardized exception handling across all services  
✅ Consistent ApiResponse wrapper for all endpoints  
✅ Service-to-service communication via OpenFeign  
✅ Eureka Service Discovery  
✅ API Gateway (Spring Cloud Gateway)  
✅ Resilience4j (Circuit Breaker + Retry + Timeout + Fallback)  
🚧 Dockerization  
🚧 Spring Cloud Config Server
