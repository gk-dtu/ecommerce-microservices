# Ecommerce Microservices

A production-style Spring Boot microservices backend with independent 
User, Product, and Order services communicating via OpenFeign.

## Tech Stack
- Java 17
- Spring Boot 3.2.12
- Spring Cloud OpenFeign (inter-service communication)
- Spring Data JPA + Hibernate
- H2 In-memory Database
- Maven

## Services

| Service         | Port | Responsibility                          |
|-----------------|------|-----------------------------------------|
| user-service    | 8081 | User registration and management        |
| product-service | 8082 | Product catalog and inventory           |
| order-service   | 8083 | Order placement, validates user and product via Feign |

## How to Run

```bash
# Terminal 1
cd user-service && mvn spring-boot:run

# Terminal 2
cd product-service && mvn spring-boot:run

# Terminal 3
cd order-service && mvn spring-boot:run
```

## API Endpoints

### User Service (port 8081)
| Method | Endpoint      | Description     |
|--------|---------------|-----------------|
| POST   | /users        | Create user     |
| PUT   | /users/{id}    | Update user     |
| GET    | /users/{id}   | Get user by ID  |
| GET    | /users        | Get all users   |

### Product Service (port 8082)
| Method | Endpoint         | Description        |
|--------|------------------|--------------------|
| POST   | /products        | Create product     |
| GET    | /products/{id}   | Get product by ID  |
| GET    | /products        | Get all products   |

### Order Service (port 8083)
| Method | Endpoint       | Description                              |
|--------|----------------|------------------------------------------|
| POST   | /orders        | Place order (validates user and product) |
| GET    | /orders/{id}   | Get order by ID                          |
| GET    | /orders        | Get all orders                           |

## Current Progress
✅ Independent microservices (User, Product, Order)

✅ REST APIs with validation and pagination

✅ Standardized exception handling across all services

✅ Service-to-service communication via OpenFeign

🚧 Eureka Service Discovery

🚧 API Gateway (Spring Cloud Gateway)

🚧 Dockerization
