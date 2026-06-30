# E-Commerce Microservices — Project Context Document

> **Purpose:** Full context handoff document. Anyone reading this should be able to understand what was planned, what is built, what decisions were made, and exactly where to continue.

---

## 1. Project Goal

Build a **production-quality e-commerce backend** using Spring Boot Microservices as a learning project. The code must resemble what exists in a real company — not tutorial code.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot **3.4.1** |
| Build | Maven (with Maven Wrapper `mvnw.cmd`) |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (dev/test) → PostgreSQL (prod/later) |
| Validation | Spring Boot Starter Validation (Hibernate Validator) |
| HTTP Client | OpenFeign + Apache HttpClient 5 (`feign-hc5`) |
| API Docs | SpringDoc OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui` **v2.8.0**) |
| Unit Tests | JUnit 5 + Mockito (via `spring-boot-starter-test`) |
| Integration Tests | `@SpringBootTest` + WireMock (`spring-cloud-contract-wiremock`) |
| Coverage Enforcement | JaCoCo — minimum **80%** line coverage |
| Service Discovery | Eureka (`spring-cloud-starter-netflix-eureka-server/client`) — ✅ Complete |
| API Gateway | Spring Cloud Gateway (reactive/WebFlux) — ✅ Complete |
| Circuit Breaker | Resilience4j — **Phase 6 (Next)** |
| Containerization | Docker + Docker Compose — ✅ Complete |
| CI/CD | GitHub Actions — **Phase 7** |
| Deployment Target | GCP (Cloud Run or GKE) — **Phase 8** |

**Spring Cloud Version:** `2024.0.1` (compatible with Spring Boot 3.4.x)

---

## 3. Planned Microservices

| Service | Port | Status |
|---|---|---|
| `product-service` | 8081 | ✅ Complete |
| `order-service` | 8082 | ✅ Complete |
| `eureka-server` | 8761 | ✅ Complete |
| `api-gateway` | 8080 | ✅ Complete |

---

## 4. Architecture Rules (enforced throughout)

- **Layered architecture** inside every service: `controller → service → repository`
- **Constructor injection only** — never `@Autowired` on fields
- **Never expose JPA entities** from controllers — always use DTOs
- **DTOs are immutable** — Lombok `@Value` + `@Builder`
- **Service interface + impl** — controller depends on interface, not concrete class
- **One `GlobalExceptionHandler`** per service via `@RestControllerAdvice`
- **No cross-service shared libraries** — each service has its own copy of DTOs it consumes from other services
- **Transactions**: class-level `@Transactional(readOnly = true)`, mutating methods override with `@Transactional`
- **SOLID principles** throughout

### Package structure per service

```
com.naveen.<servicename>/
├── controller/
├── service/
│   └── impl/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
└── config/
```

---

## 5. Phase Roadmap

| Phase | Description | Status |
|---|---|---|
| **Phase 1** | Product Service — full CRUD, validation, Swagger, unit + integration tests, Dockerfile | ✅ Done |
| **Phase 2** | Order Service — full CRUD, OpenFeign → Product Service, WireMock tests, Dockerfile | ✅ Done |
| **Phase 3** | Docker Compose — wire both services together with Docker networking | ✅ Done |
| **Phase 4** | Eureka Server — service discovery, all services register | ✅ Done |
| **Phase 5** | API Gateway — single entry point, routes to services via Eureka | ✅ Done |
| **Phase 6** | Resilience4j — circuit breaker on Feign calls in Order Service | ✅ Done |
| **Phase 7** | CI/CD — GitHub Actions: build, test, package, Docker build | ✅ Done |
| **Phase 8** | GCP Deployment — Cloud Run or GKE, externalized config, secrets | ⏳ Next |

---

## 6. What Is Built — Phase 1: Product Service

**Location:** `C:\Users\srajs\IdeaProjects\product-service\`

### Entity: `Product`

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Auto-generated primary key |
| `name` | `String` | Not null, max 255 |
| `description` | `String` | TEXT column |
| `category` | `String` | Not null, max 100 |
| `price` | `BigDecimal` | `NUMERIC(10,2)`, must be > 0 |
| `quantity` | `Integer` | Must be >= 0 |
| `createdAt` | `LocalDateTime` | Auto-set by JPA auditing, never updated |
| `updatedAt` | `LocalDateTime` | Auto-updated by JPA auditing |

### REST API

| Method | Endpoint | Description | Response |
|---|---|---|---|
| `POST` | `/api/v1/products` | Create product | `201 Created` |
| `GET` | `/api/v1/products/{id}` | Get by ID | `200 OK` / `404` |
| `GET` | `/api/v1/products?category=X` | List all (optional category filter) | `200 OK` |
| `PUT` | `/api/v1/products/{id}` | Full update | `200 OK` / `404` |
| `DELETE` | `/api/v1/products/{id}` | Delete | `204 No Content` / `404` |
| `PATCH` | `/api/v1/products/{id}/reduce-stock?quantity=N` | Reduce stock — called by Order Service | `204 No Content` / `404` / `409` |

### File Inventory

```
product-service/
├── pom.xml                                            ← Spring Boot 3.4.1, Spring Cloud BOM, JaCoCo 80%
├── Dockerfile                                         ← Multi-stage, non-root user
├── src/main/resources/
│   └── application.properties                         ← Port 8081, H2, Swagger, Actuator, Eureka config
├── src/test/resources/
│   └── application.properties                         ← eureka.client.enabled=false (disables Eureka in tests)
└── src/main/java/com/naveen/productservice/
    ├── ProductServiceApplication.java
    ├── config/
    │   ├── JpaAuditingConfig.java                     ← @EnableJpaAuditing (isolated from main class)
    │   └── OpenApiConfig.java
    ├── controller/
    │   └── ProductController.java                     ← 6 endpoints, /api/v1/products
    ├── dto/
    │   ├── ProductRequest.java                        ← @Value, validation annotations
    │   └── ProductResponse.java                       ← @Value
    ├── entity/
    │   └── Product.java                               ← @Getter @Setter @Builder (not @Data)
    ├── exception/
    │   ├── ApiErrorResponse.java
    │   ├── GlobalExceptionHandler.java                ← @RestControllerAdvice
    │   ├── InsufficientStockException.java            ← → 409 Conflict
    │   └── ResourceNotFoundException.java             ← → 404 Not Found
    ├── mapper/
    │   └── ProductMapper.java                         ← toEntity, toResponse, updateEntity
    ├── repository/
    │   └── ProductRepository.java                     ← JpaRepository + findByCategory + searchByName (JPQL)
    └── service/
        ├── ProductService.java                        ← interface
        └── impl/
            └── ProductServiceImpl.java                ← @Transactional(readOnly=true) on class

src/test/java/com/naveen/productservice/
├── controller/ProductControllerTest.java              ← @WebMvcTest, @MockitoBean, 11 tests
├── service/ProductServiceImplTest.java                ← @ExtendWith(MockitoExtension.class), 13 tests
└── integration/ProductIntegrationTest.java            ← @SpringBootTest, real H2, 4 tests
```

### Key Design Decisions

- `@Getter + @Setter` on entity (not `@Data`) — `@Data` generates `equals/hashCode` on all fields including mutable ones, which breaks JPA collections and proxy comparisons
- `@EnableJpaAuditing` in a separate `JpaAuditingConfig` class (not on `@SpringBootApplication`) — this avoids conflicts with `@WebMvcTest` slice tests
- `updateEntity(Product, ProductRequest)` in mapper modifies a **managed JPA entity in-place** — Hibernate detects dirty fields and issues a targeted `UPDATE` without needing explicit `save()`
- `BigDecimal` for price — never use `double/float` for money
- `@DecimalMin("0.01")` — price must be strictly positive, 0.00 is rejected
- `reduceStock` endpoint uses `PATCH` — partial update, not `PUT` (full replacement)
- `existsById` check before `deleteById` — Spring Data's `deleteById` silently does nothing if entity is missing; explicit check ensures 404 is thrown

---

## 7. What Is Built — Phase 2: Order Service

**Location:** `C:\Users\srajs\IdeaProjects\order-service\`

### Entity: `Order`

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Auto-generated primary key |
| `productId` | `Long` | NOT a `@ManyToOne` — cross-service FK anti-pattern |
| `quantity` | `Integer` | Ordered quantity |
| `totalPrice` | `BigDecimal` | `NUMERIC(10,2)` — computed by Order Service |
| `status` | `OrderStatus` | Enum stored as STRING: `PENDING`, `CONFIRMED`, `CANCELLED` |
| `createdAt` | `LocalDateTime` | Auto-set by JPA auditing |

**Why no `@ManyToOne Product`:** Each microservice has its own database. Cross-service foreign keys couple schemas and break service independence.

### REST API

| Method | Endpoint | Description | Response |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Place order | `201 Created` / `404` / `409` / `503` |
| `GET` | `/api/v1/orders/{id}` | Get by ID | `200 OK` / `404` |
| `GET` | `/api/v1/orders` | List all | `200 OK` |
| `PATCH` | `/api/v1/orders/{id}/cancel` | Cancel order | `200 OK` / `404` |

### Order Creation Business Flow

```
POST /api/v1/orders
    │
    ├── 1. Call product-service GET /api/v1/products/{productId}
    │       ├── 404 from product-service → throw ResourceNotFoundException → 404
    │       └── 5xx / connection error → throw ProductServiceException → 503
    │
    ├── 2. Check product.quantity >= requested quantity
    │       └── insufficient → throw InsufficientStockException → 409
    │
    ├── 3. Call product-service PATCH /api/v1/products/{id}/reduce-stock?quantity=N
    │       └── error → throw ProductServiceException → 503
    │
    ├── 4. Compute totalPrice = product.price × quantity
    │
    └── 5. Save Order with status=CONFIRMED → return 201
```

### File Inventory

```
order-service/
├── pom.xml                                            ← Spring Cloud BOM 2024.0.1, feign-hc5, Eureka client
├── Dockerfile                                         ← Multi-stage, non-root user, port 8082
├── src/main/resources/
│   └── application.properties                         ← Port 8082, feign HC5 enabled, product URL, Eureka config
├── src/test/resources/
│   └── application.properties                         ← eureka.client.enabled=false (disables Eureka in tests)
└── src/main/java/com/naveen/orderservice/
    ├── OrderServiceApplication.java                   ← @EnableFeignClients (required)
    ├── client/
    │   └── ProductServiceClient.java                  ← @FeignClient(name="product-service", url="${product.service.url:}")
    ├── config/
    │   ├── JpaAuditingConfig.java
    │   └── OpenApiConfig.java
    ├── controller/
    │   └── OrderController.java                       ← 4 endpoints, /api/v1/orders
    ├── dto/
    │   ├── OrderRequest.java                          ← productId + quantity only (no totalPrice — security)
    │   ├── OrderResponse.java
    │   └── ProductResponse.java                       ← LOCAL copy, not imported from product-service
    ├── entity/
    │   └── Order.java                                 ← table name "orders" (ORDER is SQL reserved keyword)
    ├── exception/
    │   ├── ApiErrorResponse.java
    │   ├── GlobalExceptionHandler.java                ← includes 503 for ProductServiceException
    │   ├── InsufficientStockException.java
    │   ├── ProductServiceException.java               ← thrown when Feign call fails
    │   └── ResourceNotFoundException.java
    ├── mapper/
    │   └── OrderMapper.java                           ← toResponse only (no toEntity — business logic builds entity)
    ├── repository/
    │   └── OrderRepository.java
    └── service/
        ├── OrderService.java
        └── impl/
            └── OrderServiceImpl.java

src/test/java/com/naveen/orderservice/
├── controller/OrderControllerTest.java                ← @WebMvcTest, 8 tests
├── service/OrderServiceImplTest.java                  ← @ExtendWith(MockitoExtension.class), 10 tests
└── integration/OrderIntegrationTest.java              ← @SpringBootTest + @AutoConfigureWireMock, 5 tests
```

### Key Design Decisions

- **`ProductResponse` is duplicated locally** in order-service — sharing a common-dto library would create compile-time coupling between services. Each service only declares the fields it needs; Jackson ignores extras.
- **`feign-hc5` (Apache HttpClient 5)** is required — Java's default `HttpURLConnection` blocks PATCH requests ("Invalid HTTP method: PATCH") in some JVM configurations. Apache HC5 supports all HTTP verbs.
- **`@AutoConfigureWireMock(port = 9090)`** — Spring Cloud Contract's annotation starts WireMock and wires it into the Spring test context. `@TestPropertySource(properties = "product.service.url=http://localhost:9090")` overrides the Feign URL.
- **All WireMock calls prefixed with `WireMock.`** (no wildcard static imports) — avoids compiler ambiguity with MockMvc's own `get()`, `post()`, `patch()` static methods.
- **`urlPathEqualTo`** used for PATCH stubs (not `urlMatching`) — ignores query string, more reliable matching.
- **`cancelOrder` does NOT restore stock** — deliberate. In a real system, this would be an async event (`OrderCancelledEvent`) consumed by product-service. Implemented as a stub for future event-sourcing.
- **Table name `orders`** — `ORDER` is a reserved SQL keyword; using it as a table name causes syntax errors in PostgreSQL and some H2 configurations.
- **`url = "${product.service.url:}"` (the colon)** — empty default enables Eureka discovery when the property is not set. When set to a URL, Feign bypasses Eureka and calls that URL directly. This allows the same code to work locally (direct URL) and in Docker with Eureka (empty → discovery).

---

## 8. What Is Built — Phase 3: Docker Compose

**Location:** `C:\Users\srajs\IdeaProjects\docker-compose.yml`

### What it does

- Runs all 4 services in Docker containers connected via `ecommerce-network` (Docker bridge)
- Eureka Server starts first; all other services wait for its health check to pass before starting
- Order Service waits for both Eureka Server AND Product Service to be healthy
- All services register with Eureka and are discoverable by name
- API Gateway is the single public entry point on port 8080

### Docker Compose Startup Order

```
eureka-server (8761) → healthy
        │
        ├── product-service (8081) → healthy
        │       │
        │       └── order-service (8082) → healthy
        │
        └── api-gateway (8080) → healthy
```

### Environment Variable Overrides

| Service | Variable | Value | Maps to property |
|---|---|---|---|
| All | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://eureka-server:8761/eureka` | `eureka.client.service-url.defaultZone` |
| product-service, order-service | `SPRING_H2_CONSOLE_ENABLED` | `false` | `spring.h2.console.enabled` |
| product-service, order-service | `SPRING_JPA_SHOW_SQL` | `false` | `spring.jpa.show-sql` |
| order-service | `PRODUCT_SERVICE_URL` | `""` (empty) | `product.service.url` — empty → Feign uses Eureka |

### Health Check Command

```yaml
test: ["CMD-SHELL", "wget -qO- http://localhost:<port>/actuator/health || exit 1"]
```

`wget` is available in `eclipse-temurin:17-jre-alpine`. `curl` is not installed in Alpine by default — using `wget` avoids the need to install additional packages.

---

## 9. What Is Built — Phase 4: Eureka Server

**Location:** `C:\Users\srajs\IdeaProjects\eureka-server\`

### What it does

Eureka is a **service registry** — every microservice registers itself here on startup. Other services query Eureka to discover where a service is running, instead of hard-coding IP addresses or hostnames.

### File Inventory

```
eureka-server/
├── pom.xml                  ← spring-cloud-starter-netflix-eureka-server + actuator only
├── Dockerfile               ← Multi-stage, non-root user, port 8761
└── src/main/resources/
    └── application.properties
└── src/main/java/com/naveen/eurekaserver/
    └── EurekaServerApplication.java   ← @EnableEurekaServer
```

### Key Properties

| Property | Value | Reason |
|---|---|---|
| `eureka.client.register-with-eureka` | `false` | Eureka Server must not register itself — it IS the registry |
| `eureka.client.fetch-registry` | `false` | No peer to replicate from in standalone mode |
| `eureka.server.wait-time-in-ms-when-sync-empty` | `0` | Don't delay startup waiting for peer sync |
| `eureka.server.enable-self-preservation` | `false` | In dev, remove stale registrations immediately when a service goes down |

### Changes Made to Existing Services

Both `product-service` and `order-service` received:
1. `spring-cloud-dependencies` BOM added to `pom.xml`
2. `spring-cloud-starter-netflix-eureka-client` dependency added
3. `eureka.client.service-url.defaultZone=http://localhost:8761/eureka` added to `application.properties`
4. `src/test/resources/application.properties` created with `eureka.client.enabled=false` — prevents connection errors during tests

---

## 10. What Is Built — Phase 5: API Gateway

**Location:** `C:\Users\srajs\IdeaProjects\api-gateway\`

### What it does

The API Gateway is the **single public entry point**. All external traffic goes through port 8080. The Gateway looks up services in Eureka and load-balances requests across instances.

### File Inventory

```
api-gateway/
├── pom.xml                  ← spring-cloud-starter-gateway, eureka-client, loadbalancer, actuator
├── Dockerfile               ← Multi-stage, non-root user, port 8080
└── src/main/resources/
    └── application.yml      ← Routes defined here (YAML preferred over .properties for nested config)
└── src/main/java/com/naveen/apigateway/
    └── ApiGatewayApplication.java   ← @SpringBootApplication only (no extra annotations needed)
```

### Route Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://product-service       # lb:// = load-balanced via Eureka
          predicates:
            - Path=/api/v1/products/**

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
```

### Key Design Decisions

- **No `spring-boot-starter-web` in pom.xml** — Spring Cloud Gateway uses WebFlux (reactive Netty). Adding MVC alongside causes a startup conflict ("Cannot have both reactive and servlet web stacks on the classpath").
- **`lb://` URI prefix** — tells Gateway to use Spring Cloud LoadBalancer to resolve the service name from Eureka, rather than treating it as a fixed host.
- **`discovery.locator.enabled: true`** — auto-generates routes for every registered Eureka service. The explicit routes above take priority but this acts as a catch-all.
- **`lower-case-service-id: true`** — Eureka stores service names uppercase (`PRODUCT-SERVICE`); this lowercases them so auto-generated route URLs stay clean.
- **`/actuator/gateway/routes`** — exposed endpoint to inspect all active routes at runtime.

### API Gateway Routing Map

| External URL (port 8080) | Routes to |
|---|---|
| `http://localhost:8080/api/v1/products/**` | `http://product-service:8081/api/v1/products/**` |
| `http://localhost:8080/api/v1/orders/**` | `http://order-service:8082/api/v1/orders/**` |

---

## 11. What Is Built — Phase 6: Resilience4j Circuit Breaker

**Files changed:** `order-service` only.

### Goal

Wrap the two Feign calls in `order-service` (`getProductById` + `reduceStock`) with a circuit breaker. When `product-service` is down or slow, the circuit opens and subsequent requests fail fast with a structured error response instead of hanging until a TCP timeout.

### File Inventory

```
order-service/
├── pom.xml                                               ← added spring-cloud-starter-circuitbreaker-resilience4j
├── src/main/resources/application.properties            ← CB enabled + Resilience4j config + CB actuator health
├── src/test/resources/application.properties            ← added CB + HC5 properties (critical: test classpath shadows main)
└── src/main/java/com/naveen/orderservice/
    ├── client/
    │   ├── ProductServiceClient.java                     ← added fallbackFactory = ProductServiceClientFallbackFactory.class
    │   └── ProductServiceClientFallbackFactory.java      ← NEW — translates all Feign exceptions into domain exceptions
    └── service/impl/
        └── OrderServiceImpl.java                         ← removed try-catch from helpers; fallback factory owns error translation
```

### How the Circuit Breaker Works

```
ProductServiceClient call
        │
        ▼
  CB checks state
        │
  ┌─────┴──────┐
CLOSED        OPEN
  │              │
  │ make call    │ skip call, call fallback immediately
  │              │  (cause = CallNotPermittedException)
  │ success → record success
  │ failure → record failure
        │
  failure rate ≥ 50% after min 5 calls
        │
        ▼
      OPEN  ──── wait 10s ──── HALF-OPEN ──── 3 probe calls ──── all pass ──► CLOSED
                                                                  any fail  ──► OPEN
```

### Circuit Breaker Configuration

| Property | Value | Meaning |
|---|---|---|
| `sliding-window-size` | 10 | Track last 10 calls |
| `failure-rate-threshold` | 50 | Open if ≥50% of calls fail |
| `minimum-number-of-calls` | 5 | Need at least 5 calls before evaluating |
| `wait-duration-in-open-state` | 10s | Stay OPEN for 10 seconds before probing |
| `permitted-number-of-calls-in-half-open-state` | 3 | Allow 3 probe calls in HALF-OPEN |
| `automatic-transition-from-open-to-half-open-enabled` | true | No call needed to trigger HALF-OPEN transition |

### FallbackFactory vs Fallback

`FallbackFactory<T>` (used here) receives the **cause** exception — so the fallback can behave differently:

| `cause` type | Fallback action |
|---|---|
| `FeignException.NotFound` (404) | Throw `ResourceNotFoundException` → 404 |
| `CallNotPermittedException` (CB OPEN) | Log "circuit open", throw `ProductServiceException` → 503 |
| Any other `FeignException` | Throw `ProductServiceException` → 503 |

A plain `@FeignClient(fallback = ...)` (without Factory) does not receive the cause — only use it when you always return the same fallback regardless of why the call failed.

### Key Design Decisions

- **`spring.cloud.openfeign.circuitbreaker.group.enabled=true`** — groups all methods of `ProductServiceClient` under one CB named `product-service`. Without this, each method gets its own CB (`ProductServiceClient#getProductById(Long)`, `ProductServiceClient#reduceStock(Long,int)`), requiring separate config per method.
- **`OrderServiceImpl` try-catch removed** — with the fallback factory active, `FeignException` never reaches `OrderServiceImpl`. The fallback owns all exception translation. The helpers become one-liners.
- **Unit tests updated** — `@ExtendWith(MockitoExtension.class)` tests have no Spring context and no CB infrastructure. Tests that previously mocked `FeignException` now mock the domain exceptions directly (`ResourceNotFoundException`, `ProductServiceException`), which matches the contract the fallback factory provides in production.
- **CB health exposed via Actuator** — `GET /actuator/health` now includes circuit breaker state. Set `management.health.circuitbreakers.enabled=true`.

### New Actuator Endpoint (Circuit Breaker State)

`GET http://localhost:8082/actuator/health`
```json
{
  "circuitBreakers": {
    "status": "UP",
    "details": {
      "product-service": {
        "state": "CLOSED",
        "failureRate": "0.0%",
        "bufferedCalls": 3,
        "slowCalls": 0
      }
    }
  }
}
```

---

## 12. HTTP Error Mapping Reference

| Exception | HTTP Status | Thrown By |
|---|---|---|
| `ResourceNotFoundException` | `404 Not Found` | product not found, order not found |
| `InsufficientStockException` | `409 Conflict` | stock check fails |
| `ProductServiceException` | `503 Service Unavailable` | Feign call fails (non-404) |
| `MethodArgumentNotValidException` | `400 Bad Request` | `@Valid` fails on request body |
| `Exception` (catch-all) | `500 Internal Server Error` | unexpected runtime errors |

---

## 12. Test Summary

| Service | Unit Tests | Controller Tests | Integration Tests | Total |
|---|---|---|---|---|
| product-service | 13 | 11 | 4 | **28** |
| order-service | 10 | 8 | 5 | **23** |

Both services: **BUILD SUCCESS**, all tests green, JaCoCo 80% enforced.

**Note:** `eureka-server` and `api-gateway` have no application tests — they are infrastructure services with no business logic. Their correctness is validated through end-to-end Docker Compose testing.

**Eureka in tests:** Both `product-service` and `order-service` have `src/test/resources/application.properties` that sets `eureka.client.enabled=false`. This prevents Spring from attempting Eureka connections during unit and integration tests, which would cause `ConnectException` noise and slow test startup.

---

## 13. How to Run Locally

### Option A — Individual services (no Docker)

#### Prerequisites
- Java 17+
- Maven (or use `mvnw.cmd`)

#### Start order: Eureka first, then services

```bash
# Terminal 1 — Eureka Server
cd eureka-server
mvnw.cmd spring-boot:run
# Dashboard: http://localhost:8761

# Terminal 2 — Product Service
cd product-service
mvnw.cmd spring-boot:run
# Swagger: http://localhost:8081/swagger-ui.html
# H2 Console: http://localhost:8081/h2-console  (JDBC URL: jdbc:h2:mem:productdb)

# Terminal 3 — Order Service
cd order-service
mvnw.cmd spring-boot:run
# Swagger: http://localhost:8082/swagger-ui.html

# Terminal 4 — API Gateway
cd api-gateway
mvnw.cmd spring-boot:run
# Health: http://localhost:8080/actuator/health
# Routes: http://localhost:8080/actuator/gateway/routes
```

All traffic can now go through gateway on port 8080.

### Option B — Docker Compose (all 4 services, recommended)

#### Prerequisites
- Docker Desktop installed and running

```bash
# From C:\Users\srajs\IdeaProjects\
docker compose up --build
```

Wait for all 4 health checks to pass (~2-3 minutes on first build). Then:

| Endpoint | URL |
|---|---|
| Eureka Dashboard | `http://localhost:8761` |
| Gateway Health | `http://localhost:8080/actuator/health` |
| Gateway Routes | `http://localhost:8080/actuator/gateway/routes` |
| Products via Gateway | `http://localhost:8080/api/v1/products` |
| Orders via Gateway | `http://localhost:8080/api/v1/orders` |

```bash
# Stop everything
docker compose down

# Rebuild a single service after code change
docker compose up --build product-service

# View logs
docker compose logs -f order-service
```

### Run Tests

```bash
mvnw.cmd test                        # all tests
mvnw.cmd test -Dtest=ClassName       # single class
mvnw.cmd verify                      # tests + JaCoCo coverage check (80% threshold)
```

---

## 14. What Is Built — Phase 7: GitHub Actions CI/CD

**Repository:** `https://github.com/rajsreeworks/ecommerce-microservices`

**Registry:** GitHub Container Registry (`ghcr.io`) — uses built-in `GITHUB_TOKEN`, no extra accounts needed.

### What triggers the pipeline

| Event | Tests run | Docker images pushed |
|---|---|---|
| Push to `main` | Yes | Yes — only after tests pass |
| Pull Request to `main` | Yes | No |

### Pipeline structure

```
push to main
      │
      ├── test-product-service (~36s with cache)   ./mvnw verify → 28 tests + JaCoCo 80%
      │
      └── test-order-service   (~38s with cache)   ./mvnw verify → 22 tests + JaCoCo 80%
                      │
                      └── both must pass
                                │
                    build-and-push (4 services in parallel)
                    ├── ghcr.io/rajsreeworks/product-service:latest + :<sha>
                    ├── ghcr.io/rajsreeworks/order-service:latest + :<sha>
                    ├── ghcr.io/rajsreeworks/eureka-server:latest + :<sha>
                    └── ghcr.io/rajsreeworks/api-gateway:latest + :<sha>
```

### File Inventory

```
.github/workflows/ci.yml    ← full pipeline: test jobs + docker matrix build-and-push
.gitignore                  ← covers target/, .idea/, *.iml, logs/, .env, OS files
```

### Key Decisions

- **`needs: [test-product-service, test-order-service]`** — Docker push blocked if any test job fails
- **`if: github.event_name == 'push' && github.ref == 'refs/heads/main'`** — PRs test only, never push images
- **`actions/setup-java@v4` with `cache: maven`** — caches `~/.m2`; subsequent runs ~36-38s
- **Matrix strategy** — all 4 Docker builds run in parallel; total time stays ~3 min regardless of service count
- **Double tags (`:latest` + `:<git-sha>`)** — `:latest` for convenience, `:<sha>` for reproducible production deploys

---

## 15. What Comes Next — Phase 8: GCP Deployment

### Goal

Deploy all 4 containers to Google Cloud Platform so the API is live on the public internet.

### Recommended approach: Cloud Run

Cloud Run is serverless — you give it a container image, it handles everything else (scaling, HTTPS, load balancing). No Kubernetes needed for this project.

### What's needed (you do these)

1. **Create a GCP account** at `console.cloud.google.com` (free tier available)
2. **Create a new project** e.g. `ecommerce-microservices`
3. **Enable APIs**: Cloud Run, Container Registry
4. **Install `gcloud` CLI**: `https://cloud.google.com/sdk/docs/install`
5. Tell me your GCP project ID — I'll build the full deploy config

### What's already done (no work needed)

- `/actuator/health` endpoints → Cloud Run uses these for health checks
- `XX:+UseContainerSupport` JVM flag → Cloud Run is containerized, JVM adapts automatically
- Non-root user in Dockerfiles → GCP security best practice already followed
- Docker images live at `ghcr.io/rajsreeworks/*:latest` → Cloud Run can pull directly

---

## 16. Future Reference

_(Nothing planned beyond Phase 8 for this project)_

---

## 16. Important Gotchas Discovered During Implementation

| # | Issue | Root Cause | Fix |
|---|---|---|---|
| 1 | `@EnableJpaAuditing` on `@SpringBootApplication` breaks `@WebMvcTest` | `@WebMvcTest` loads main class but skips JPA layer → "No auditing infrastructure present" | Move `@EnableJpaAuditing` to a separate `@Configuration` class |
| 2 | `@Data` on JPA entities causes JPA collection/proxy issues | `@Data` generates `equals/hashCode` on all mutable fields | Use `@Getter + @Setter` on entities, never `@Data` |
| 3 | `deleteById` silently ignores missing entities | Spring Data JPA design | Explicitly call `existsById` first and throw `ResourceNotFoundException` |
| 4 | WireMock wildcard `import static WireMock.*` conflicts with MockMvc static imports | Both have `get()`, `post()`, `patch()` static methods | Never use wildcard imports from WireMock. Prefix all WireMock calls with `WireMock.` |
| 5 | Feign `PATCH` throws "Invalid HTTP method: PATCH" | `HttpURLConnection` (Feign's default) blocks PATCH in some JVMs | Add `feign-hc5` dependency and `spring.cloud.openfeign.httpclient.hc5.enabled=true` |
| 6 | Table name `order` causes SQL errors | `ORDER` is a reserved SQL keyword | Use `@Table(name = "orders")` |
| 7 | WireMock `urlMatching` doesn't reliably match PATCH + query params | Regex anchoring behavior in WireMock 2.x | Use `urlPathEqualTo` for path matching (ignores query string) |
| 8 | `BigDecimal` vs `double` for prices | Floating-point precision errors with `double` | Always use `BigDecimal` with `precision=10, scale=2` |
| 9 | SpringDoc 2.6.0 returns 500 on `/api-docs` with Spring Boot 3.4.1 | Spring Boot 3.4 changed MVC auto-configuration internals; SpringDoc 2.6.0 is incompatible | Upgrade `springdoc.version` to `2.8.0` in both service pom.xml files |
| 10 | Eureka client tries to connect during tests, causing `ConnectException` noise | Auto-configuration fires for all `@SpringBootTest` and `@WebMvcTest` when Eureka is on classpath | Add `src/test/resources/application.properties` with `eureka.client.enabled=false` |
| 11 | `spring-boot-starter-web` + `spring-cloud-starter-gateway` conflict | Gateway is WebFlux (Netty); adding MVC creates dual-stack conflict Spring cannot resolve | API Gateway pom.xml must NOT include `spring-boot-starter-web` |
| 12 | `@FeignClient(url = "${property}")` bypasses Eureka even in Docker | Non-empty `url` attribute disables Eureka discovery entirely, regardless of Eureka being present | Use `url = "${product.service.url:}"` (colon = empty default). Set the env var to empty in Docker to activate Eureka discovery |
| 13 | `src/test/resources/application.properties` silently shadows the main one on the test classpath | Maven puts test-classpath ahead of main-classpath; Spring Boot finds and loads only the first `application.properties` it sees | Any Spring/Feign/Cloud property that is only in the main properties file is NOT active in tests. Repeat all critical non-business properties in the test file |
| 14 | `fallbackFactory` on `@FeignClient` does nothing without `spring.cloud.openfeign.circuitbreaker.enabled=true` | The fallback factory attribute is silently ignored if Feign's circuit breaker support is not enabled | Add `spring.cloud.openfeign.circuitbreaker.enabled=true` to both main and test `application.properties`. Also add `circuitbreaker.group.enabled=true` to group all methods of one client under a single CB |
| 15 | After removing try-catch from `OrderServiceImpl`, unit tests that mock `FeignException` break | Unit tests (`@ExtendWith(MockitoExtension.class)`) have no Spring context and no CB infrastructure, so the fallback factory is never called. Raw `FeignException` bubbles up uncaught | In unit tests, mock the domain exceptions directly (`ResourceNotFoundException`, `ProductServiceException`) — this tests `OrderServiceImpl`'s contract, not Feign internals |
| 16 | `mvnw` committed from Windows fails in Linux CI with "Permission denied" | Git on Windows commits files as `100644` (not executable). Linux Docker runners cannot run `./mvnw` | Run `git update-index --chmod=+x */mvnw` once per service to record `100755` in the git index. All future Linux checkouts will have +x automatically |

---

## 17. Project Directory Layout

```
C:\Users\srajs\IdeaProjects\           (git root → github.com/rajsreeworks/ecommerce-microservices)
├── .github/workflows/ci.yml           ✅ Complete (Phase 7 — CI/CD)
├── .gitignore
├── docker-compose.yml                 ✅ Complete (Phase 3 — all 4 services)
├── ECOMMERCE_PROJECT_CONTEXT.md       ← this document
├── product-service\                   ✅ Complete (Phase 1 + Eureka client)
├── order-service\                     ✅ Complete (Phase 2 + Eureka client + Resilience4j)
├── eureka-server\                     ✅ Complete (Phase 4)
└── api-gateway\                       ✅ Complete (Phase 5)
```

---

## 18. Manual API Testing — Postman Guide

### Option A — Test via Docker Compose (all traffic through Gateway on port 8080)

#### Start

```bash
cd C:\Users\srajs\IdeaProjects
docker compose up --build
```

Wait until all 4 services appear on the Eureka Dashboard at `http://localhost:8761`.

Verify gateway is up:
- `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- `GET http://localhost:8080/actuator/gateway/routes` → JSON list of active routes

All Postman requests below work on **port 8080** when using Docker Compose (Gateway routes them internally to the correct service).

---

### Option B — Test individual services directly

Open terminals as described in Section 13 Option A. Use ports 8081 and 8082 directly.

---

### Step 1 — Verify Both Services Are Up

| URL | Expected |
|---|---|
| `http://localhost:8081/actuator/health` (or `8080`) | `{"status":"UP"}` |
| `http://localhost:8082/actuator/health` (or `8080`) | `{"status":"UP"}` |

---

### Step 2 — Postman Setup

- Create a new **Collection** called `E-Commerce API`
- Inside it, create two folders: `Product Service` and `Order Service`
- Set header `Content-Type: application/json` on every POST/PUT/PATCH request
- Create a collection variable `BASE_URL` = `http://localhost:8080` (Gateway) or `http://localhost:8081` for direct

---

### Step 3 — Product Service Endpoints

#### 3.1 Create Product

| | |
|---|---|
| Method | `POST` |
| URL | `{{BASE_URL}}/api/v1/products` |
| Body | raw → JSON |

```json
{
  "name": "Laptop Pro",
  "description": "High performance laptop",
  "category": "Electronics",
  "price": 1299.99,
  "quantity": 50
}
```

Expected: `201 Created`
```json
{
  "id": 1,
  "name": "Laptop Pro",
  "category": "Electronics",
  "price": 1299.99,
  "quantity": 50,
  "createdAt": "...",
  "updatedAt": "..."
}
```
**Save the `id` — needed for all subsequent requests.**

---

#### 3.2 Create a Second Product

`POST {{BASE_URL}}/api/v1/products`

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "category": "Electronics",
  "price": 29.99,
  "quantity": 200
}
```
Expected: `201 Created`

---

#### 3.3 Get Product by ID

`GET {{BASE_URL}}/api/v1/products/1` → `200 OK`

`GET {{BASE_URL}}/api/v1/products/999` → `404 Not Found`
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: '999'",
  "path": "/api/v1/products/999"
}
```

---

#### 3.4 List All Products

`GET {{BASE_URL}}/api/v1/products` → `200 OK` — array of all products.

`GET {{BASE_URL}}/api/v1/products?category=Electronics` → filtered list.

---

#### 3.5 Update Product

| | |
|---|---|
| Method | `PUT` |
| URL | `{{BASE_URL}}/api/v1/products/1` |
| Body | raw → JSON |

```json
{
  "name": "Laptop Pro X",
  "description": "Updated description",
  "category": "Electronics",
  "price": 1499.99,
  "quantity": 45
}
```
Expected: `200 OK` with updated values.

---

#### 3.6 Validation Error Test

`POST {{BASE_URL}}/api/v1/products`

```json
{
  "name": "",
  "category": "Electronics",
  "price": 0,
  "quantity": 10
}
```
Expected: `400 Bad Request`
```json
{
  "status": 400,
  "message": "Validation failed",
  "validationErrors": {
    "name": "Product name is required",
    "price": "Price must be greater than 0"
  }
}
```

---

#### 3.7 Reduce Stock

`PATCH {{BASE_URL}}/api/v1/products/1/reduce-stock?quantity=5`

Expected: `204 No Content` (empty body).

Verify: `GET /api/v1/products/1` — quantity drops by 5.

**Test insufficient stock:**
`PATCH {{BASE_URL}}/api/v1/products/1/reduce-stock?quantity=9999`
Expected: `409 Conflict`

---

#### 3.8 Delete Product

`DELETE {{BASE_URL}}/api/v1/products/2`

Expected: `204 No Content`

Verify: `GET /api/v1/products/2` → `404 Not Found`

---

### Step 4 — Order Service Endpoints

> Product Service must be running and product ID `1` must exist with quantity > 0.

---

#### 4.1 Create Order — Happy Path

| | |
|---|---|
| Method | `POST` |
| URL | `{{BASE_URL}}/api/v1/orders` |
| Body | raw → JSON |

```json
{
  "productId": 1,
  "quantity": 3
}
```

Expected: `201 Created`
```json
{
  "id": 1,
  "productId": 1,
  "quantity": 3,
  "totalPrice": 4499.97,
  "status": "CONFIRMED",
  "createdAt": "..."
}
```

**Behind the scenes:**
1. Order Service called `GET /api/v1/products/1` via Feign (or Eureka if Docker)
2. Checked stock (45 >= 3 ✓)
3. Called `PATCH /api/v1/products/1/reduce-stock?quantity=3`
4. Computed totalPrice = 1499.99 × 3 = 4499.97
5. Saved order with status `CONFIRMED`

Verify stock reduced: `GET /api/v1/products/1` — quantity should be `42`.

---

#### 4.2 Create Order — Insufficient Stock

```json
{ "productId": 1, "quantity": 9999 }
```
Expected: `409 Conflict`

---

#### 4.3 Create Order — Product Does Not Exist

```json
{ "productId": 99999, "quantity": 1 }
```
Expected: `404 Not Found`

---

#### 4.4 Create Order — Validation Error

```json
{ "productId": 1, "quantity": 0 }
```
Expected: `400 Bad Request`

---

#### 4.5 Get Order by ID

`GET {{BASE_URL}}/api/v1/orders/1` → `200 OK`

`GET {{BASE_URL}}/api/v1/orders/999` → `404 Not Found`

---

#### 4.6 List All Orders

`GET {{BASE_URL}}/api/v1/orders` → `200 OK` — array

---

#### 4.7 Cancel Order

`PATCH {{BASE_URL}}/api/v1/orders/1/cancel`

Expected: `200 OK` — status changes to `CANCELLED`

**Cancel again:**
`PATCH {{BASE_URL}}/api/v1/orders/1/cancel`
Expected: `500` — `Order 1 is already cancelled`

---

### Step 5 — Test Service-Down Scenario

Stop product-service. Then:

`POST {{BASE_URL}}/api/v1/orders` → `503 Service Unavailable`
```json
{
  "status": 503,
  "message": "Unable to reach Product Service. Please try again later."
}
```

With Resilience4j added (Phase 6), subsequent calls will return the fallback response immediately without waiting for a timeout.

---

### Step 6 — Swagger UI

| Service | Direct URL | Via Gateway |
|---|---|---|
| Product Service | `http://localhost:8081/swagger-ui.html` | N/A (Gateway doesn't proxy Swagger UI) |
| Order Service | `http://localhost:8082/swagger-ui.html` | N/A |
| Eureka Dashboard | `http://localhost:8761` | N/A |
| Gateway Routes | N/A | `http://localhost:8080/actuator/gateway/routes` |

---
