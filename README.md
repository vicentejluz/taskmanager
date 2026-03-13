# Task Manager API

A full-featured RESTful task management backend built with **Java 25** and **Spring Boot 4**, featuring JWT-based authentication, role-based access control, email verification, and a complete infrastructure stack with Docker Compose.

> **Note:** This is an academic project developed for learning purposes.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Environment Variables](#environment-variables)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
  - [Authentication](#authentication-apiv1auth)
  - [Tasks](#tasks-apiv1tasks)
  - [Users](#users-apiv1users)
- [Project Structure](#project-structure)
- [Database Migrations](#database-migrations)
- [Security](#security)
- [Scheduled Jobs](#scheduled-jobs)
- [License](#license)

---

## Features

- ✅ **Task Management** — Create, update, filter, and manage tasks with statuses (`PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED`)
- 🔐 **JWT Authentication** — Stateless auth with short-lived access tokens and rotating refresh tokens (stored as HTTP-only cookies)
- 📧 **Email Verification** — Account verification and password-reset flows via email
- 🛡️ **Role-Based Access Control** — `USER` and `ADMIN` roles with protected admin endpoints
- 🔒 **Account Locking** — Automatic account lock after consecutive failed login attempts
- ♻️ **Token Management** — JWT blacklisting on logout via Redis
- 📄 **Pagination & Filtering** — Paginated responses with support for multiple filter criteria
- 📊 **Observability** — Centralised logging with the ELK stack (Elasticsearch, Logstash, Kibana)
- 📬 **Email Testing** — Mailpit SMTP server for local development
- 🗄️ **Database Versioning** — Flyway-managed SQL migrations

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Build | Maven (wrapper included) |
| Database | PostgreSQL 18.3 |
| ORM / Migration | Spring Data JPA (Hibernate) + Flyway |
| Caching | Redis 8.6.1 |
| Authentication | Spring Security + JJWT 0.13.0 |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Logging | Logback + Logstash |
| Monitoring | Elasticsearch 9 + Kibana 9 |
| Email | Spring Mail + Mailpit (dev) |
| Containerisation | Docker Compose |

---

## Prerequisites

- **JDK 25+**
- **Docker & Docker Compose** (for the full infrastructure stack)
- A `.env` file in the project root (see [Environment Variables](#environment-variables))

---

## Getting Started

### Environment Variables

Create a `.env` file in the project root with the following variables:

```env
# PostgreSQL
PG_DB=taskmanager_db
PG_USER=taskmanager_user
PG_PASSWORD=your_secure_password

# Redis
REDIS_PASSWORD=your_redis_password

# JWT
JWT_TOKEN=your_secret_jwt_key_at_least_32_chars
JWT_ISSUER=taskmanager

# Initial admin account
ADMIN_PASSWORD=initial_admin_password

# Mailpit (dev email server)
MAILPIT_USER=mailpit_user
MAILPIT_PASSWORD=mailpit_password
```

### Running with Docker Compose

Spin up all infrastructure services (PostgreSQL, Redis, ELK stack, Mailpit):

```bash
docker-compose up -d
```

| Service | URL |
|---------|-----|
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Elasticsearch | `http://localhost:9200` |
| Kibana | `http://localhost:5601` |
| Mailpit UI | `http://localhost:8025` |

### Running the Application

```bash
# Build
./mvnw clean package -DskipTests

# Run (uses the dev profile by default)
./mvnw spring-boot:run

# Run tests
./mvnw test
```

The application starts on **`http://localhost:8080`** with Swagger UI available at the root path.

---

## API Documentation

Interactive API docs are available at **`http://localhost:8080`** (Swagger UI) when the application is running. The raw OpenAPI spec is served at `/v3/api-docs`.

### Authentication (`/api/v1/auth`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/register` | Register a new user account | Public |
| `POST` | `/login` | Authenticate and receive a JWT + refresh token cookie | Public |
| `POST` | `/resend-email-verification` | Resend account verification email | Public |
| `POST` | `/forgot-password` | Initiate password-reset flow | Public |
| `GET` | `/verify-email?token=` | Verify email address with token | Public |
| `GET` | `/password-reset?token=` | Complete password reset with token | Public |
| `POST` | `/refresh` | Exchange refresh token cookie for a new access token | Public |
| `POST` | `/logout` | Revoke refresh token and blacklist it in Redis | Authenticated |

### Tasks (`/api/v1`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tasks/{id}` | Get a task by ID (owner only) | User |
| `GET` | `/tasks` | List own tasks (filterable by `status`, `due-date`; paginated) | User |
| `POST` | `/tasks` | Create a new task | User |
| `PATCH` | `/tasks/{id}` | Update task details | User |
| `PATCH` | `/tasks/{id}/done` | Mark task as done | User |
| `PATCH` | `/tasks/{id}/cancel` | Cancel a task | User |
| `GET` | `/admin/tasks/{id}` | Get any task by ID | Admin |
| `DELETE` | `/admin/tasks/{id}/delete` | Hard-delete a task | Admin |
| `GET` | `/admin/users/{userId}/tasks` | List all tasks for a specific user | Admin |

### Users (`/api/v1`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/users/me` | Get own profile | User |
| `PATCH` | `/users/me` | Update own profile (name, email) | User |
| `PATCH` | `/users/me/password` | Change password | User |
| `DELETE` | `/users/me/delete` | Delete own account | User |
| `GET` | `/admin/users/{id}` | Get a user by ID | Admin |
| `GET` | `/admin/users/by-email` | Find a user by email | Admin |
| `GET` | `/admin/users` | List all users (filterable by `name`, `accountStatus`; paginated) | Admin |
| `PATCH` | `/admin/users/{id}/enabled` | Enable or disable a user account | Admin |

---

## Project Structure

```
src/main/java/com/vicente/taskmanager/
├── config/          # Spring Security, Redis, OpenAPI, and other config
├── controller/      # REST controllers (Auth, Task, User)
├── domain/
│   ├── entity/      # JPA entities (Task, User, RefreshToken, VerificationToken)
│   └── enums/       # TaskStatus, UserRole, AccountStatus, …
├── dto/
│   ├── request/     # Incoming request DTOs
│   ├── response/    # Outgoing response DTOs
│   └── filter/      # Filter/query parameter DTOs
├── exception/       # Custom exceptions and global exception handler
├── mapper/          # Entity ↔ DTO mappers
├── repository/      # Spring Data JPA repositories and specifications
├── scheduler/       # Scheduled background jobs
├── security/        # JWT filter, auth providers, permission checkers
├── service/         # Business logic (interfaces + implementations)
└── validation/      # Custom constraint validators
```

---

## Database Migrations

Database schema is managed by **Flyway** and applied automatically on startup. Migration scripts live in:

```
src/main/resources/db/migration/
```

The migrations create and evolve the following tables:

| Table | Description |
|-------|-------------|
| `users` | User accounts (credentials, roles, account status) |
| `tasks` | Task items (title, description, status, due date, owner) |
| `roles` | Role assignments (`USER`, `ADMIN`) |
| `verification_tokens` | Time-limited email verification tokens |
| `refresh_tokens` | JWT refresh tokens with expiry |

`updated_at` columns are automatically maintained by PostgreSQL triggers.

---

## Security

| Mechanism | Details |
|-----------|---------|
| **Access Token** | JWT, expires in **10 minutes** |
| **Refresh Token** | Rotated on each use, expires in **12 days**, stored as HTTP-only cookie |
| **Logout** | Refresh token is blacklisted in Redis immediately |
| **Account Locking** | Account locks after **5** consecutive failed login attempts; base lockout period is **30 minutes** (exponentially back-off) |
| **Email Verification** | New accounts must verify their email before they can log in |
| **Password Reset** | Time-limited token sent to the registered email address |
| **RBAC** | Method-level security with `USER` and `ADMIN` roles |

---

## Scheduled Jobs

The application runs four background schedulers:

| Scheduler | Schedule | Purpose |
|-----------|----------|---------|
| Task Scheduler | `00:00:10` daily | Clean up expired / stale tasks |
| User Scheduler | `01:00:20` daily | Purge inactive / soft-deleted users |
| Token Scheduler | Configurable | Remove expired verification tokens |
| Refresh Token Scheduler | Configurable | Purge expired refresh tokens |

Cron expressions can be customised in `application.properties`.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
