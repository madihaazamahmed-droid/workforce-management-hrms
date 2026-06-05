# Workforce Management System
### Construction HR — Attendance & Overtime Settlement Engine

---

## What this is

A Spring Boot backend built for a construction HR company. Tracks daily worker attendance across sites, calculates overtime with tiered rates and monthly caps, and settles overtime in atomic transactions with SMS notifications that only fire after a successful database commit.

**Base:** Built from scratch following the DeepThought assignment spec. Not a fork of an existing HRMS — the suggested repo (amigoscode/spring-boot-fullstack) was reviewed for structural patterns but the codebase here is purpose-built for the construction use case.

**Why from scratch vs fork:** The amigoscode repo's entity model is built around employee records with no concept of sites, shifts, or wage-based overtime. Retrofitting it would have meant replacing almost every entity and service. Starting clean was the faster, cleaner path.

---

## Stack

- Java 17 + Spring Boot 3.2
- Hibernate/JPA with PostgreSQL (Supabase)
- Redis (active workers cache, 16-hour TTL safety net)
- Spring Security (stateless, CORS configured at filter chain level)

---

## Setup

### 1. Supabase (PostgreSQL)

1. Create a free project at [supabase.com](https://supabase.com)
2. Go to **Project Settings → Database → Connection string**
3. **Use the Connection Pooler URL (port 6543)** — not the direct connection (port 5432)
   - The pooler uses PgBouncer and is essential under any real load
   - Direct connections bypass PgBouncer and exhaust Supabase's ~15 connection limit quickly
4. Copy the pooler URL. It looks like:
   ```
   postgresql://postgres.xxxx:[password]@aws-0-ap-south-1.pooler.supabase.com:6543/postgres
   ```

### 2. Redis

Local:
```bash
docker run -d -p 6379:6379 redis:7
```
Or use [Upstash](https://upstash.com) free tier for a cloud instance.

### 3. Environment Variables

```bash
export DB_URL="jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres"
export DB_USERNAME="postgres.xxxx"
export DB_PASSWORD="your-supabase-password"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""   # leave empty for local Redis without auth
```

### 4. Run

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

App starts on `http://localhost:8080`. Hibernate auto-creates tables on first run.

### 5. Staging

```bash
SPRING_PROFILES_ACTIVE=staging mvn spring-boot:run
```
Staging profile (`application-staging.yml`) sets tighter HikariCP config for Supabase's idle timeout behavior. See notes in LF-205 below.

---

## API Reference

### Workers

```bash
# Create worker
curl -X POST http://localhost:8080/api/workers \
  -H "Content-Type: application/json" \
  -d '{"name":"Ravi Kumar","phone":"9876543210","designation":"MASON","dailyWageRate":800,"active":true}'

# Get worker
curl http://localhost:8080/api/workers/1

# Update worker (also invalidates Redis session cache)
curl -X PUT http://localhost:8080/api/workers/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Ravi Kumar","phone":"9876543210","designation":"SUPERVISOR","dailyWageRate":1200,"active":true}'
```

### Sites

```bash
curl -X POST http://localhost:8080/api/sites \
  -H "Content-Type: application/json" \
  -d '{"siteName":"Greenfield Phase 2","location":"Whitefield, Bengaluru","active":true}'
```

### Attendance

```bash
# Clock in
curl -X POST http://localhost:8080/api/attendance/clock-in \
  -H "Content-Type: application/json" \
  -d '{"workerId":1,"siteId":1}'

# Clock out
curl -X POST http://localhost:8080/api/attendance/clock-out \
  -H "Content-Type: application/json" \
  -d '{"workerId":1}'

# Active workers (Redis-only)
curl http://localhost:8080/api/attendance/active

# Attendance log (paginated)
curl "http://localhost:8080/api/attendance/log?workerId=1&from=2026-05-01&to=2026-05-31&page=0&size=20"
```

### Overtime

```bash
# Monthly summary
curl "http://localhost:8080/api/overtime/summary/1?month=2026-05"

# Settle (only past months)
curl -X POST "http://localhost:8080/api/overtime/settle/1?month=2026-04"
```

---

## Overtime Calculation Rules

| Hours worked | Rate |
|---|---|
| 0–8 hours | Standard (no overtime) |
| Hours 9–10 (first 2 OT hours) | 1.5× daily wage rate |
| Hours 11+ (beyond 2 OT hours) | 2.0× daily wage rate |

Daily wage rate ÷ 8 = hourly rate. Overtime is calculated on the hourly rate.

**Monthly cap:** 60 overtime hours per worker. If a clock-out would push a worker past 60 hours, the attendance is recorded in full but the overtime entry is capped at the remaining allowance.

**Example:** Worker with ₹800/day rate works 11 hours.
- Hourly rate: ₹100
- First 2 OT hours: 2 × ₹100 × 1.5 = ₹300
- Next 1 OT hour: 1 × ₹100 × 2.0 = ₹200
- Total OT payout: ₹500

---

## Tickets Fixed

### LF-201: CORS
CORS is configured at the Spring Security filter chain level (`SecurityConfig.java`). This is the only place that works — `@CrossOrigin` annotations are processed after the security filter rejects the OPTIONS preflight. Allowed origins are externalized to `application.yml` as `cors.allowed-origins` so dev/staging/prod can each set their own without recompiling.

### LF-202: Redis startup crash
`connect-timeout: 2000` in `application.yml` makes Redis fail-fast instead of hanging. `RedisConfig.java` implements a custom `CacheErrorHandler` that logs and swallows all Redis exceptions at runtime — the app degrades to DB-only transparently. When Redis recovers, caching resumes automatically.

### LF-203: N+1 + no pagination
`AttendanceRepository` uses `JOIN FETCH` in its query — Worker and Site load in the same DB round-trip. Pageable parameters flow from controller → service → repository. Default page size is 20, max 100. Response is wrapped in `PagedResponse<T>` with metadata. Turn on `spring.jpa.show-sql=true` locally to verify the SQL is a single joined query.

### LF-204: Partial settlement + premature SMS
`OvertimeService.settle()` is one `@Transactional` method — all entries for a worker+month commit atomically. SMS fires via `@TransactionalEventListener(phase = AFTER_COMMIT)` — if the transaction rolls back, the event never fires, so no premature SMS. SMS runs `@Async` so a failed notification doesn't affect the API response; failures are logged and queued for retry.

### LF-205: Connection pool exhaustion
`application-staging.yml` has Supabase-tuned HikariCP config: `keepalive-time: 60s`, `idle-timeout: 4min` (under Supabase's 5min kill), `max-lifetime: 8min`. Uses the pooler URL (port 6543). External API calls in `OvertimeService.getSummary()` are done **before** the `@Transactional` method opens — no DB connection is held while waiting on an external API.

---

## Design Decisions

**Why Redis is exclusive for active workers:** The active workers list needs to be fast for site supervisors clocking in 40 workers before a shift. A DB query with joins on a 50k-row table at 7am rush is the wrong answer. Redis is the right answer. The 16-hour TTL is a safety net, not the primary mechanism.

**Why settlement can't be partial:** Payroll systems downstream consume settled overtime as a batch. A half-settled month creates an inconsistent state that payroll software either ignores or double-counts. All-or-nothing is the only semantically correct behavior.

**Why the 60-hour cap is enforced at clock-out, not as a query:** The cap check at clock-out time (using `sumOvertimeHoursForMonth`) is a single aggregation query. Enforcing it post-hoc would require recalculating every entry — correct, but expensive and error-prone at month boundaries.

**Schema:** `@Index` annotations on every foreign key and frequently-filtered column. Enums stored as `STRING` in Postgres, not ordinals — ordinal storage breaks silently when enum order changes.

**What I'd do differently with more time:**
- Add JWT authentication — the current config permits all `/api/**` for simplicity
- Add a scheduled job to sweep expired Redis sessions (TTL handles expiry, but flagging missed clock-outs in the DB needs a cron)
- Add integration tests with Testcontainers for Redis and PostgreSQL
- Implement retry queue for failed SMS notifications (Redis list or SQS)

---

## AI Tools Used

- **Claude (Anthropic):** Used to generate boilerplate entity/DTO/config code and to cross-check the LF-ticket fixes (especially the @TransactionalEventListener pattern and HikariCP parameters for Supabase). All business logic (overtime calculation, monthly cap, settlement atomicity) was reasoned through manually and verified against the spec before implementation.
- **GitHub Copilot:** Used for autocomplete on repetitive Lombok/JPA patterns.

Specific places where I overrode AI suggestions:
- AI suggested using `@Cacheable` on the active-workers endpoint. Wrong — `@Cacheable` caches method return values keyed by parameters, not a live shared state. Replaced with manual `RedisTemplate` operations on a shared set.
- AI generated the HikariCP config with `idle-timeout: 600000` (10min). Supabase kills idle connections at ~5min. Changed to 4min (240000ms) with keepalive at 60s.
- AI initially placed the SMS call inside the `@Transactional` settlement method. This is the exact bug described in LF-204. Moved to `@TransactionalEventListener(AFTER_COMMIT)`.
