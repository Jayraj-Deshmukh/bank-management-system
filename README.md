# Bank Management System Backend

Production-style, enterprise RESTful banking backend application built with **Java 21** and **Spring Boot 3.3.5**.

---

## Key Features

* **Authentication & Authorization**: Stateless JWT authentication supporting `CUSTOMER` and `ADMIN` roles with BCrypt password hashing.
* **Account Management**: Auto-generated 12-digit account numbers supporting `SAVINGS` and `CHECKING` types.
* **Deposit & Withdrawal**: Atomic balance updates with `BigDecimal` monetary precision and `InsufficientBalanceException` guards.
* **Concurrency-Safe Fund Transfer**: Atomic multi-account transfers using database pessimistic write locking (`PESSIMISTIC_WRITE`) and deterministic lock ordering to prevent circular lock-order deadlocks.
* **Paginated Transaction History**: Database-level pagination (`Pageable`), sorting (newest first), type filtering (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER_OUT`, `TRANSFER_IN`), and date range filtering.
* **Admin Banking Operations**: Customer management, account listing, status management (`ACTIVE`, `INACTIVE`, `FROZEN`, `CLOSED`), and system-wide transaction auditing.
* **OpenAPI / Swagger 3.0 Documentation**: Interactive API documentation at `/swagger-ui.html`.

---

## Technology Stack

* **Language**: Java 21 (LTS)
* **Framework**: Spring Boot 3.3.5 (Spring MVC, Spring Security, Spring Data JPA)
* **Security**: JSON Web Tokens (JJWT 0.12.6) & BCrypt
* **Database**: MySQL 8.0 & Hibernate ORM 6.5
* **Documentation**: Springdoc OpenAPI 2.5.0
* **Build Tool**: Maven

---

## Architecture Overview

```text
Client / Swagger UI
      ↓
Spring Security (JwtAuthenticationFilter)
      ↓
REST Controllers (AuthController, AccountController, TransactionController, AdminController)
      ↓
Service Layer (@Transactional Business Logic, Deterministic Account Locking, Validation)
      ↓
Spring Data JPA Repositories (Database-Level Pagination & Filters)
      ↓
MySQL 8.0 Database
```

---

## API Authorization Matrix

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/health` | `GET` | `PUBLIC` | Health check endpoint |
| `/api/auth/register` | `POST` | `PUBLIC` | Customer registration |
| `/api/auth/login` | `POST` | `PUBLIC` | User login (issues JWT token) |
| `/v3/api-docs/**`, `/swagger-ui.html` | `GET` | `PUBLIC` | OpenAPI Swagger documentation |
| `/api/accounts` | `POST` | `CUSTOMER` | Create new bank account |
| `/api/accounts/my` | `GET` | `CUSTOMER` | View authenticated user's accounts |
| `/api/accounts/{accountNumber}` | `GET` | `CUSTOMER` / `ADMIN` | View account details |
| `/api/accounts/{accountNumber}/deposit` | `POST` | `CUSTOMER` / `ADMIN` | Deposit cash |
| `/api/accounts/{accountNumber}/withdraw` | `POST` | `CUSTOMER` / `ADMIN` | Withdraw cash |
| `/api/accounts/transfer` | `POST` | `CUSTOMER` | Concurrency-safe fund transfer |
| `/api/accounts/{accountNumber}/transactions` | `GET` | `CUSTOMER` / `ADMIN` | View paginated transaction history |
| `/api/admin/customers` | `GET` | `ADMIN` | List all system customers |
| `/api/admin/accounts` | `GET` | `ADMIN` | List all system accounts |
| `/api/admin/accounts/{accountNumber}/status` | `PATCH` | `ADMIN` | Activate / Deactivate account status |
| `/api/admin/transactions` | `GET` | `ADMIN` | Audit system-wide transaction log |
| `/api/admin/transactions/{ref}` | `GET` | `ADMIN` | View detailed transaction record |

---

## Transfer Concurrency & Deadlock Prevention Strategy

To ensure thread-safety during simultaneous fund transfers:
1. **Pessimistic Locking**: Account entities are locked at the database level via `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`).
2. **Deterministic Lock Ordering**: Source and destination account numbers are lexicographically compared (`accountA.compareTo(accountB)`). Locks are always acquired in ascending account number order, preventing circular lock-order deadlocks for opposite-direction transfers (Thread 1: A → B, Thread 2: B → A).

---

## Setup & Running Instructions

### Prerequisites
* Java 21 JDK
* Maven 3.8+
* MySQL 8.0 Server

### Environment Variables
Configure the required environment variables:
* `DB_URL` (e.g. `jdbc:mysql://localhost:3306/bank_management_db`)
* `DB_USERNAME` (e.g. `root`)
* `DB_PASSWORD` (Database password)
* `JWT_SECRET` (Secure 256-bit HMAC secret key)
* `JWT_EXPIRATION` (Optional token expiration in ms, default: `86400000`)
* `PORT` (Optional server port, default: `8080`)

### Running the Application
```bash
cd backend
mvn clean package -DskipTests
java -jar target/bank-management-system-0.0.1-SNAPSHOT.jar
```

### Running Tests
```bash
cd backend
mvn clean test
```

---

## Interactive API Documentation

Once the server is running, access Swagger UI in your browser:
* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Specs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
