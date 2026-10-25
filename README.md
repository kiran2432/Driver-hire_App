# Driver Hire

Customers post a trip (one-way or round trip, car details, pickup/destination) and hire a driver. Drivers register with their experience and licence details, browse open trips in their city, and accept requests.

**Stack:** Spring Boot 4 (Java 21), Spring Security (stateless JWT + HttpOnly refresh cookie), JPA + Flyway, PostgreSQL · React 19 + TypeScript + Vite.

```
backend/    Spring Boot API   (auth, user, driver, customer, trip packages; Flyway migrations)
frontend/   React SPA         (served by nginx in production, which also proxies /api to the backend)
docker-compose.yml            Postgres + backend + web
```

## How it works

1. **Register** as a driver (name, phone, address, city, state, years of experience, licence number/expiry, gearbox skills, languages, daily rate, bio) or as a customer (name, phone, email, password, address, city, state).
2. A **customer** creates a trip: type (`ONE_WAY` / `ROUND_TRIP`, return time required for round trips), pickup city/location, destination, start time, car make/model/type/transmission, notes.
3. The customer **finds drivers** and hires one → trip becomes `REQUESTED`. Drivers can also claim `OPEN` trips directly from their dashboard (customer details are visible to drivers in the same city).
4. The driver **accepts** → `CONFIRMED`. Phone numbers of both sides are revealed only at this point. The driver can decline (trip returns to `OPEN`) or mark the trip `COMPLETED`; the customer can cancel until completion.

Rules enforced server-side: a driver must drive the car's transmission and hold a licence valid on the trip date; trips in the past can't be taken; two drivers racing for one open trip → the loser gets `409`; users only see trips they own, are assigned to, or (drivers) that are open.

## Run locally (no setup)

```bash
# backend: in-memory H2 + demo data
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev      # http://localhost:8080
# frontend
cd frontend && npm ci && npm run dev                                   # http://localhost:5173 (proxies /api)
```

Demo accounts (password `Password123`): `customer@example.com`, `ravi@example.com`, `suresh@example.com`, `meena@example.com` (drivers).

## Run in production-like mode (Docker)

```bash
cp .env.example .env      # set DB_PASSWORD and JWT_SECRET (openssl rand -base64 48)
docker compose up -d --build
# open http://localhost:8081
```

Only the `web` container is published. Put a TLS terminator / load balancer in front of it and keep `COOKIE_SECURE=true`.

### Configuration (environment variables)

| Variable | Required | Purpose |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | yes | PostgreSQL connection |
| `JWT_SECRET` | yes | HS256 signing key, ≥ 32 chars. The app refuses to start without it |
| `COOKIE_SECURE` | no (default `true`) | `Secure` flag on the refresh cookie |
| `CORS_ALLOWED_ORIGINS` | no | Only if the SPA is served from a different origin than the API |
| `AUTH_RATE_LIMIT_PER_MINUTE` | no (default 30) | Per-IP limit on `/api/auth/**` |
| `DB_POOL_SIZE` | no (default 10) | Hikari pool size |

## Production-readiness notes

- Schema owned by Flyway (`ddl-auto: validate`); no demo data outside the `dev` profile.
- BCrypt (cost 12) passwords; 15-minute access tokens; rotating refresh tokens in an HttpOnly, SameSite=Strict cookie scoped to `/api/auth`; expired refresh tokens cleaned up on a schedule.
- Per-IP rate limiting on auth endpoints. It is in-memory, so with several backend replicas add a shared limiter at the gateway.
- Error responses never leak messages/stack traces; graceful shutdown; `/actuator/health` (with liveness/readiness probes) is the only exposed actuator surface.
- nginx adds CSP and other security headers, long-cache for fingerprinted assets, SPA fallback.
- Containers run as non-root; CI (GitHub Actions) runs backend tests, frontend typecheck/tests/build and Docker builds.

**Not included (add as needed):** email verification / password reset, payments, ratings & reviews, driver document upload and verification, push/SMS notifications, a shared (Redis) rate limiter, centralized logging/metrics export, automated database backups.

## API summary

`/api/auth/{register/driver,register/customer,login,refresh,logout}` · `/api/me` · `/api/customers/me` · `/api/drivers` (search), `/api/drivers/{id}`, `/api/drivers/me`, `PATCH /api/drivers/me/availability` · `/api/trips` (create), `/mine`, `/open`, `/assigned`, `/{id}`, `/{id}/{hire|cancel|accept|decline|complete}`
