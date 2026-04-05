# Finance Dashboard API

A production-grade REST API for a personal/organisational Finance Dashboard, built with Spring Boot 3.x, Spring Security 6 (stateless JWT), Spring Data JPA, PostgreSQL, and Flyway.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Setup & Run — Local](#setup--run--local)
6. [Setup & Run — Docker](#setup--run--docker)
7. [Running Tests](#running-tests)
8. [API Reference](#api-reference)
9. [Authentication Flow](#authentication-flow)
10. [Role & Access Control](#role--access-control)
11. [Design Decisions](#design-decisions)
12. [Known Limitations & Future Improvements](#known-limitations--future-improvements)

---

## Project Overview

The Finance Dashboard API allows users to track income and expenses, view financial summaries, analyse spending by category, and see monthly trends. It supports three access tiers — **VIEWER**, **ANALYST**, and **ADMIN** — each with distinct capabilities enforced via JWT and Spring Security's `@PreAuthorize`.

---

## Tech Stack

| Layer           | Technology                                    |
|-----------------|-----------------------------------------------|
| Language        | Java 17                                       |
| Framework       | Spring Boot 3.3.x                             |
| Security        | Spring Security 6 + JJWT 0.12.x (HS256)      |
| Persistence     | Spring Data JPA + Hibernate + PostgreSQL 15   |
| Migrations      | Flyway                                        |
| Build           | Maven 3.9+                                    |
| Mapping         | MapStruct 1.5.x                               |
| Validation      | Jakarta Bean Validation 3.x                   |
| API Docs        | SpringDoc OpenAPI 3 (Swagger UI)              |
| Testing         | JUnit 5 + Mockito                             |
| Code reduction  | Lombok                                        |

---

## Project Structure

```
src/
├── main/
   ├── java/com/finance/dashboard/
   │   ├── FinanceDashboardApplication.java
   │   ├── config/          # SecurityConfig, OpenApiConfig, CorsConfig
   │   ├── controller/      # AuthController, UserController,
   │   │                    # TransactionController, DashboardController
   │   ├── service/         # AuthService, UserService,
   │   │                    # TransactionService, DashboardService
   │   ├── repository/      # UserRepository, TransactionRepository,
   │   │                    # TransactionSpecification
   │   ├── entity/          # User, Transaction
   │   ├── dto/
   │   │   ├── request/     # LoginRequest, RegisterRequest, ...
   │   │   └── response/    # ApiResponse, AuthResponse, ...
   │   ├── mapper/          # UserMapper, TransactionMapper (MapStruct)
   │   ├── security/        # JwtTokenProvider, JwtAuthenticationFilter,
   │   │                    # CustomUserDetailsService, SecurityContextHelper
   │   ├── exception/       # GlobalExceptionHandler + custom exceptions
   │   ├── enums/           # UserRole, TransactionType, TransactionCategory
   │   └── util/            # DateRangeUtil, PaginationUtil
   └── resources/
       ├── application.yml
       └── db/migration/
           ├── V1__init_schema.sql
           └── V2__seed_data.sql
```

---

## Prerequisites

- **Java 17+** — `java -version` must show 17 or higher
- **Maven 3.9+** — `mvn -version`
- **PostgreSQL 13+** — running on `localhost:5432`
- A PostgreSQL database named `finance_dashboard`:
  ```sql
  CREATE DATABASE finance_dashboard;
  ```

---

## Setup & Run — Local

### 1. Clone the repository
```bash
git clone https://github.com/your-org/finance-dashboard.git
cd finance-dashboard
```

### 2. Configure database credentials
Edit `src/main/resources/application.yml` (or use environment variables):
```yaml
spring:
  datasource:
    url:      jdbc:postgresql://localhost:5432/finance_dashboard
    username: postgres
    password: postgres
```

Or override via environment variables at runtime:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance_dashboard
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=mypassword
```

### 3. Change the JWT secret (production)
In `application.properties`, replace the placeholder with a real 256-bit base64-encoded secret:
```bash
# Generate a secure secret
openssl rand -base64 32
```
Then set `app.jwt.secret` to the output.

### 4. Build the project
```bash
mvn clean package -DskipTests
```

### 5. Run Flyway migrations + start the server
```bash
mvn spring-boot:run
```
Flyway automatically applies `V1__init_schema.sql` and `V2__seed_data.sql` on first start.

### 6. Verify startup
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health check:** http://localhost:8080/actuator/health
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### 7. Log in with seeded accounts

| Email                  | Password      | Role    |
|------------------------|---------------|---------|
| admin@finance.com      | Admin@123     | ADMIN   |
| analyst@finance.com    | Analyst@123   | ANALYST |
| viewer@finance.com     | Viewer@123    | VIEWER  |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.com","password":"Admin@123"}'
```

---

## Setup & Run — Docker

The easiest way to run the project. No need to install PostgreSQL or configure anything — Docker handles it all.

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### 1. Clone the repository
```bash
git clone https://github.com/your-org/finance-dashboard.git
cd finance-dashboard
```

### 2. Build and start
```bash
docker compose up --build
```
This will:
- Pull the PostgreSQL 15 image
- Build the Spring Boot app image
- Run Flyway migrations automatically
- Seed the database with 3 users and 18 sample transactions

Run in the background:
```bash
docker compose up --build -d
```

### 3. Verify both containers are running
```bash
docker compose ps
```
Both `finance_postgres` and `finance_app` should show as **healthy**.

### 4. Verify the API is up
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health check:** http://localhost:8080/actuator/health

### 5. Log in with seeded accounts

| Email | Password | Role |
|---|---|---|
| admin@finance.com | Admin@123 | ADMIN |
| analyst@finance.com | Analyst@123 | ANALYST |
| viewer@finance.com | Viewer@123 | VIEWER |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.com","password":"Admin@123"}'
```

### Useful Docker commands

```bash
# Watch live logs
docker compose logs -f

# Watch only app logs
docker compose logs -f app

# Get a shell inside the app container
docker exec -it finance_app sh

# Connect to the database directly
docker exec -it finance_postgres psql -U postgres -d finance_dashboard

# Stop containers (keeps database data)
docker compose down

# Stop and wipe the database (full reset)
docker compose down -v

# Rebuild after code changes
docker compose up --build app
```

### Troubleshooting

| Problem | Fix |
|---|---|
| Port 5432 already in use | Stop local Postgres: `sudo service postgresql stop` |
| Port 8080 already in use | Find and kill: `lsof -i :8080` |
| App crashes on startup | Run `docker compose logs app` — usually a DB timing issue, retry |
| Data looks wrong | Reset with `docker compose down -v` then `docker compose up --build` |

---

## API Reference

### Authentication

| Method | Path                 | Auth Required | Role | Description                          |
|--------|----------------------|---------------|------|--------------------------------------|
| POST   | /api/auth/register   | No            | —    | Self-register (always VIEWER role)   |
| POST   | /api/auth/login      | No            | —    | Login, returns JWT token             |

### Users

| Method | Path                          | Role    | Description                       |
|--------|-------------------------------|---------|-----------------------------------|
| GET    | /api/users                    | ADMIN   | Paginated list of all users       |
| GET    | /api/users/{id}               | ADMIN / Self | Get user by ID               |
| POST   | /api/users                    | ADMIN   | Create user with explicit role    |
| PUT    | /api/users/{id}               | ADMIN   | Partial update (name, role, active)|
| DELETE | /api/users/{id}               | ADMIN   | Soft-deactivate user              |
| PATCH  | /api/users/{id}/activate      | ADMIN   | Re-activate a deactivated user    |
| PATCH  | /api/users/{id}/deactivate    | ADMIN   | Deactivate a user                 |

### Transactions

| Method | Path                    | Role             | Description                              |
|--------|-------------------------|------------------|------------------------------------------|
| POST   | /api/transactions        | ANALYST, ADMIN   | Create transaction (owned by caller)     |
| GET    | /api/transactions        | All              | List (filtered + paginated); scoped by role |
| GET    | /api/transactions/{id}   | All              | Get by ID; scoped by role                |
| PUT    | /api/transactions/{id}   | ANALYST (own), ADMIN | Partial update                       |
| DELETE | /api/transactions/{id}   | ANALYST (own), ADMIN | Soft-delete                          |

**GET /api/transactions query parameters:**

| Parameter      | Type    | Default          | Description                              |
|----------------|---------|------------------|------------------------------------------|
| page           | int     | 0                | Page index (0-based)                     |
| size           | int     | 10               | Page size (1–100)                        |
| sortBy         | string  | transactionDate  | amount / transactionDate / category / createdAt |
| sortDir        | string  | desc             | asc / desc                               |
| type           | enum    | —                | INCOME or EXPENSE                        |
| category       | enum    | —                | SALARY / FOOD / RENT / etc.              |
| startDate      | date    | —                | yyyy-MM-dd                               |
| endDate        | date    | —                | yyyy-MM-dd                               |
| minAmount      | decimal | —                | Minimum transaction amount               |
| maxAmount      | decimal | —                | Maximum transaction amount               |
| search         | string  | —                | Fuzzy match on description               |
| userId         | UUID    | —                | ADMIN only: filter by specific user      |

### Dashboard

| Method | Path                              | Role | Description                                      |
|--------|-----------------------------------|------|--------------------------------------------------|
| GET    | /api/dashboard/summary            | All  | totalIncome, expenses, net, count, average       |
| GET    | /api/dashboard/category-breakdown | All  | Per-category totals, percentages, counts         |
| GET    | /api/dashboard/monthly-trend      | All  | 12 months of income/expenses/net for a year      |
| GET    | /api/dashboard/recent-activity    | All  | N most recent transactions (default 5, max 20)   |
| GET    | /api/dashboard/top-categories     | All  | Top N categories by amount (default EXPENSE, n=5)|

---

## Authentication Flow

```
Client                                   Server
  │                                        │
  │  POST /api/auth/login                  │
  │  { email, password }  ──────────────►  │
  │                                        │  AuthenticationManager validates credentials
  │                                        │  JwtTokenProvider.generateToken(userId, email, role)
  │  ◄──────────────────────────────────── │
  │  { token, tokenType, expiresIn, user } │
  │                                        │
  │  GET /api/transactions                 │
  │  Authorization: Bearer <token> ──────► │
  │                                        │  JwtAuthenticationFilter extracts + validates token
  │                                        │  SecurityContext populated with UserDetails
  │  ◄──────────────────────────────────── │
  │  { success, data: [...] }              │
```

Token details:
- Algorithm: **HS256**
- Expiry: **24 hours** (configurable via `app.jwt.expiration-ms`)
- Claims: `sub` (userId), `email`, `role`, `iat`, `exp`
- **No refresh token** — re-login required after expiry

---

## Role & Access Control

| Action                       | VIEWER | ANALYST | ADMIN |
|------------------------------|:------:|:-------:|:-----:|
| Self-register                | ✓      | ✓       | ✓     |
| View own transactions        | ✓      | ✓       | ✓     |
| View all transactions        | ✗      | ✗       | ✓     |
| Create transactions          | ✗      | ✓       | ✓     |
| Update own transactions      | ✗      | ✓       | ✓     |
| Update any transaction       | ✗      | ✗       | ✓     |
| Soft-delete own transactions | ✗      | ✓       | ✓     |
| Soft-delete any transaction  | ✗      | ✗       | ✓     |
| View own dashboard           | ✓      | ✓       | ✓     |
| View all-user dashboard      | ✗      | ✗       | ✓     |
| Manage users (CRUD)          | ✗      | ✗       | ✓     |

> **Important:** Access violations always return **403 Forbidden** — never 404 — to prevent callers from probing the existence of resources they are not authorised to access.

---

## Design Decisions

1. **Transactions belong to their creator.** `user_id` in the `transactions` table is the UUID of the user who called the create endpoint. There is no concept of "account" ownership.

2. **Soft-delete only.** Neither users nor transactions are ever physically deleted. `is_deleted = true` and `is_active = false` are the deletion signals. This preserves audit history and avoids orphaned references.

3. **Dashboard aggregations always exclude soft-deleted records.** The repository JPQL queries include `AND t.isDeleted = FALSE` unconditionally.

4. **ANALYST cannot see other users' transactions.** Only ADMIN can query across user boundaries. ANALYST is scoped to `user_id = caller.id` via `TransactionSpecification`.

5. **403 not 404 for ownership violations.** When a VIEWER or ANALYST requests a transaction they do not own, the service always returns `AccessDeniedException` (403). This prevents resource-existence probing.

6. **Email is immutable.** Once a user registers, their email cannot be changed via the API. `UpdateUserRequest` intentionally excludes an email field.

7. **Self-registration always yields VIEWER.** Even if a `role` field is passed in `RegisterRequest`, `AuthService.register()` unconditionally sets `user.setRole(UserRole.VIEWER)`. Only ADMIN can elevate roles.

8. **JWT has no refresh mechanism.** Tokens expire after 24 hours and re-login is required. This is a deliberate simplicity trade-off — refresh token rotation adds significant complexity.

9. **Pagination defaults:** page=0, size=10, sorted by `transactionDate DESC`.

10. **MapStruct + Lombok ordering.** The Maven Compiler Plugin annotation processor chain is ordered: Lombok first, then `lombok-mapstruct-binding`, then MapStruct. This ensures MapStruct sees Lombok-generated getters/setters/builders.

---

## Known Limitations & Future Improvements

| Limitation | Suggested Improvement |
|------------|----------------------|
| No JWT refresh token | Implement refresh token rotation with a `refresh_tokens` table |
| Password reset not implemented | Add `/api/auth/forgot-password` + email OTP flow |
| No email verification on register | Send confirmation email via Spring Mail |
| H2 dialect differences in tests | Migrate to Testcontainers with real PostgreSQL for integration tests |
| Monthly trend JPQL uses `MONTH()` / `YEAR()` functions | Not portable across all JPA dialects; consider native queries or DB views |
| No rate limiting | Add Bucket4j or Spring Cloud Gateway rate limiting |
| No audit log | Add a `transaction_audit_log` table via Hibernate Envers |
| JWT secret hardcoded in yml | Rotate via HashiCorp Vault or AWS Secrets Manager in production |
| Single-tenant | Extend to multi-tenant by adding an `organisation_id` FK to both tables |
| No soft-delete filter at JPA level | Add a `@FilterDef` / `@Filter` Hibernate soft-delete filter to avoid repetition |
