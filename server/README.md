# AstroChakra Accounting — Node backend

Express + `pg` API that replaces direct Supabase access from the browser. It runs
on cPanel shared hosting ("Setup Node.js App"), which only offers Node/Python (no
Java). It implements the same contract as [`src/api.js`](../src/api.js) — a
drop-in replacement for `src/supabase.js` — and ports the validated business logic
from the Java blueprint in [`backend/`](../backend) (which stays as reference).

## Layout

| File          | Purpose                                                            |
| ------------- | ----------------------------------------------------------------- |
| `app.js`      | Express entry point: CORS, JSON, auth middleware, error handler.  |
| `routes.js`   | All `/api/*` routes (mirrors `src/api.js`).                        |
| `db.js`       | `pg` Pool to Supabase + `withTx()` for atomic money operations.   |
| `auth.js`     | JWT sign/verify, `requireAuth`/`requireAdmin`, `login()`.         |

## Auth

Stateless JWT (Bearer token). Public: `GET /api/health`, `POST /api/auth/login`.
`/api/users/**` is admin-only; everything else needs a valid token. Legacy
plaintext passwords in `app_users` are transparently re-hashed with bcrypt on
first successful login.

## Money model — "Available = bank balance"

Every expense reduces the bank balance regardless of fund source; reserve/project
allocations are display-only. The atomic endpoints (`/api/money/*`,
`/api/salary-payments`, `/api/inventory/stock-movement`) do the insert + balance
+ reserve + allocation in a single DB transaction.

## Local dev

```sh
cd server
npm install
cp .env.example .env   # fill in DATABASE_URL + JWT_SECRET
npm start              # http://localhost:3001/api/health
```

> The password's `@` must be URL-encoded as `%40` in `DATABASE_URL`. Do **not**
> append `?sslmode=...` — SSL (`rejectUnauthorized: false`, required by the
> Supabase pooler's self-signed chain) is set in `db.js`.

## cPanel deployment

1. Upload `server/` (via Git or FTP) to the app root.
2. cPanel → **Setup Node.js App**: set Application startup file = `app.js`,
   then **Run NPM Install**.
3. Add environment variables: `DATABASE_URL`, `JWT_SECRET`, `CORS_ORIGINS`
   (Passenger sets `PORT` automatically).
4. Map the app to a subdomain or an `/api` path, then point the React app's
   `VITE_API_BASE` at it (or serve same-origin `/api`).

## Schema notes

- Bigint identity PKs (DB-generated): `balance`, `projects`, `people`,
  `income_sources`, `transactions`, `app_users`, `activity_log`,
  `change_requests`, `removals`.
- UUID PKs (DB default): `company_info`, `resources`, `salary_payments`,
  `expense_categories`, `inventory_*`.
- `db.js` parses `numeric`→number, `int8`→number, `date`→`'YYYY-MM-DD'` string so
  responses match what the old supabase-js client returned.
