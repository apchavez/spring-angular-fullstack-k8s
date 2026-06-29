[![CI](https://github.com/apchavez/spring-webflux-hexagonal-arch/actions/workflows/ci.yml/badge.svg)](https://github.com/apchavez/spring-webflux-hexagonal-arch/actions/workflows/ci.yml)

# Reactive Customer Service API

Reactive REST API built with **Spring Boot WebFlux** for full CRUD customer management, following **Hexagonal Architecture (Ports & Adapters)**.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language / Runtime | Java 21, Spring Boot 3.5.3 |
| Reactivity | Spring WebFlux (Mono / Flux), Spring Data R2DBC |
| Database | H2 (dev profile) / PostgreSQL 16 (prod profile) |
| Security | Spring Security (headers, CORS, rate limiting) |
| Observability | Spring Boot Actuator, SLF4J + Logback, X-Request-Id |
| API Docs | Springdoc OpenAPI 2 (Swagger UI) |
| Build | Gradle 8, JaCoCo (≥ 80% on domain and application) |
| Code quality | ArchUnit, SonarQube |
| Containerization | Docker (multistage build) + docker-compose |
| Orchestration | Kubernetes (manifests in `k8s/`) |
| CI/CD | GitHub Actions → ghcr.io |

---

## Architecture

Hexagonal (Ports & Adapters) with three well-defined layers:

```
src/main/java/com/apchavez/customers
├── domain
│   ├── model          Customer (record with invariants), CustomerState
│   ├── exception      Typed domain exceptions
│   ├── port           CustomerRepositoryPort (interface — the contract)
│   └── service        CustomerDomainService (pure business logic)
├── application
│   └── CustomerApplicationService  (orchestration, audit logging, @Transactional)
└── infrastructure
    ├── config         Security, RateLimiting, RequestLogging, OpenApi, Startup
    ├── mapper         CustomerMapper (DTO ↔ Domain ↔ Entity)
    ├── persistence    CustomerEntity, CustomerR2dbcRepository, CustomerPersistenceAdapter
    └── web            CustomerController, DTOs (Request/Update/Response), GlobalExceptionHandler
```

**Dependency rule:** `infrastructure` → `application` → `domain`  
The domain has no knowledge of outer layers. Verified automatically by `ArchitectureTest` (ArchUnit).

---

## Getting Started

### Local (H2 in-memory)

```bash
./gradlew bootRun
```

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Docker (PostgreSQL)

```bash
# Optional: customize credentials
cp .env.example .env
# edit .env with your values

docker compose up --build
```

> The `docker/postgres-init.sql` script initializes the PostgreSQL schema automatically on first startup.

---

## API Endpoints

Base path: `/api/v1/customers`

| Method | Route | Description | Responses |
|---|---|---|---|
| `POST` | `/` | Create customer | `201`, `400`, `422` |
| `GET` | `/active` | List active customers | `200` |
| `GET` | `/{id}` | Find by ID | `200`, `404` |
| `PUT` | `/{id}` | Full update | `200`, `400`, `404`, `422` |
| `DELETE` | `/{id}` | Delete customer | `204`, `404` |

Full interactive documentation (with request/response examples and all error codes) is available in Swagger UI.

### Quick Examples

```bash
# Create customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Alex","apellido":"Prieto","estado":"ACTIVE","edad":30}'

# List active
curl http://localhost:8080/api/v1/customers/active

# Find by ID
curl http://localhost:8080/api/v1/customers/1

# Update
curl -X PUT http://localhost:8080/api/v1/customers/1 \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Alexander","apellido":"Prieto Chavez","estado":"INACTIVE","edad":31}'

# Delete
curl -X DELETE http://localhost:8080/api/v1/customers/1
```

---

## Testing

```bash
./gradlew test          # runs all tests + JaCoCo
```

| Type | Class | Description |
|---|---|---|
| Domain model — unit + property-based (jqwik) | `CustomerDomainTest` | `Customer` record invariants |
| JSON serialization — property-based | `CustomerResponseDTOSerializationTest` | Round-trip without data loss |
| Domain service — unit | `CustomerDomainServiceTest` | Business logic (create/find/update/delete) |
| Application service — unit | `CustomerApplicationServiceTest` | Use case orchestration |
| Persistence adapter — `@DataR2dbcTest` | `CustomerPersistenceAdapterTest` | Persistence port with real H2 |
| REST controller — full integration | `CustomerControllerIntegrationTest` | All endpoints and response codes |
| Rate limiter — unit | `RateLimitingFilterTest` | Per-IP limit and IP isolation |
| Actuator probes | `ActuatorHealthTest` | Liveness/Readiness |
| Hexagonal architecture — ArchUnit | `ArchitectureTest` | 4 dependency rules enforced |

---

## CI/CD

The GitHub Actions pipeline (`.github/workflows/ci.yml`) runs three jobs on every push:

| Job | Trigger | What it does |
|---|---|---|
| `test` | Every push / PR | Compile, run tests, JaCoCo ≥ 80%, SonarQube analysis (if `SONAR_HOST_URL` is configured) |
| `k8s-validate` | Every push / PR | Validate manifests with **kubeconform** |
| `docker` | Push to `main` (after `test`) | Build + push to **ghcr.io** with `latest` and `sha-XXXXXXX` tags |

Published image:

```
ghcr.io/apchavez/spring-webflux-hexagonal-arch:latest
ghcr.io/apchavez/spring-webflux-hexagonal-arch:sha-abc1234
```

### SonarQube (optional)

To enable SonarQube analysis in CI, configure in the GitHub repository:
- **Secret:** `SONAR_TOKEN` — token generated in your SonarQube instance
- **Variable:** `SONAR_HOST_URL` — instance URL (e.g. `https://sonarcloud.io`)

Locally:
```bash
./gradlew sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.token=YOUR_TOKEN
```

---

## Kubernetes

The manifests in `k8s/` are production-ready:

| File | Description |
|---|---|
| `namespace.yaml` | `customer-service` namespace |
| `configmap.yaml` | Non-sensitive configuration (profile, DB host) |
| `secret.yaml` | Database credentials (base64) |
| `deployment.yaml` | 2 replicas, ghcr.io image, probes, resource limits, securityContext |
| `service.yaml` | ClusterIP on port 80 |
| `ingress.yaml` | NGINX Ingress at `customer-service.local` |

Apply in order:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

Kubernetes probes point to Actuator endpoints:
- **Liveness:** `GET /actuator/health/liveness`
- **Readiness:** `GET /actuator/health/readiness`

> Update `k8s/secret.yaml` with real credentials before deploying to production.  
> The Ingress assumes an NGINX Ingress Controller and host `customer-service.local`.

---

## Postman

| File | Purpose |
|---|---|
| `reactive-customer-service.postman_collection.json` | Main collection with automatic test scripts |
| `reactive-customer-service.local.postman_environment.json` | Local environment — `baseUrl = http://localhost:8080` |
| `reactive-customer-service.k8s.postman_environment.json` | Kubernetes environment — `baseUrl = http://customer-service.local` |

---

## What This Project Demonstrates

- Reactive programming end-to-end: WebFlux controllers → R2DBC repository → PostgreSQL (no blocking I/O anywhere)
- Hexagonal architecture with ArchUnit tests enforcing dependency rules at build time
- Exhaustive test coverage: unit, integration, property-based, and architectural tests
- Production Kubernetes manifests with health probes, resource limits, and security context
- Multi-stage Docker build + automated publish to GHCR on every merge to main
- Per-IP rate limiting at the filter layer, tested in isolation

---

## Related Projects

| Project | Description |
|---|---|
| [quarkus-react-fullstack-k8s](https://github.com/apchavez/quarkus-react-fullstack-k8s) | Fullstack Java 21 application with Quarkus backend, React frontend, MongoDB, Redis, and Kubernetes |
| [clean-arch-azure-functions-java](https://github.com/apchavez/clean-arch-azure-functions-java) | Java 21 serverless platform on Azure Functions with Clean Architecture |
