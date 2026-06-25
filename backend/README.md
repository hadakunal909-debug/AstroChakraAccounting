# AstroChakra Accounting — Java backend (Spring Boot)

Java/Spring Boot API that owns all data access and business logic. The React app
(repo root) calls this API instead of talking to Supabase directly.

## Requirements
- Java 21 (JDK)
- Maven 3.9+

## Run locally (no external DB — H2 smoke test)
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Then:
- `GET  http://localhost:8080/api/health`  → `{ "status": "ok", ... }`
- `GET  http://localhost:8080/api/balance` → `{ "balance": 0, "liquid_reserve": 0 }`
- `PUT  http://localhost:8080/api/balance` body `{ "balance": 1000 }`

## Run against the real Supabase Postgres
Set these environment variables (never commit them), then run `mvn spring-boot:run`:

| Variable | Example |
| --- | --- |
| `SUPABASE_DB_URL` | `jdbc:postgresql://db.<project>.supabase.co:5432/postgres?sslmode=require` |
| `SUPABASE_DB_USER` | `postgres` |
| `SUPABASE_DB_PASSWORD` | (your database password, Supabase → Settings → Database) |
| `JWT_SECRET` | a random string of at least 32 bytes |
| `CORS_ORIGINS` | `http://localhost:5173` (dev) |

The default profile uses `spring.jpa.hibernate.ddl-auto=validate`, so Hibernate
**never changes your schema** — it only verifies the entities match the existing tables.

## Build a jar
```
mvn clean package
java -jar target/accounting-backend-0.0.1-SNAPSHOT.jar
```
