# Deployment

This app has two parts that deploy to **different** places:

| Part | What it is | Where it goes |
| --- | --- | --- |
| **Frontend** (`src/`, Vite/React) | Static files after `npm run build` | **cPanel** `public_html` (via GitHub Actions) |
| **Backend** (`backend/`, Spring Boot/Java) | A running Java server | **A Java-capable host** (NOT standard cPanel) |

> **Why not the backend on cPanel?** Standard cPanel shared hosting serves PHP/static
> files and cannot run a Java/Spring Boot process. Only deploy the Java app to cPanel if
> your plan explicitly has "Setup Java App" (Tomcat) — otherwise use one of the hosts below.

---

## Frontend → cPanel (automated)

Workflow: [.github/workflows/deploy-frontend-cpanel.yml](.github/workflows/deploy-frontend-cpanel.yml).
On every push to `feat/project-fund-source` (or run it manually from the Actions tab), GitHub
Actions runs `npm ci && npm run build` and uploads `dist/` to cPanel over FTPS.

**One-time setup — add these repository secrets** (GitHub → Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `CPANEL_FTP_SERVER` | Your FTP host, e.g. `ftp.yourdomain.com` (from cPanel → FTP Accounts) |
| `CPANEL_FTP_USERNAME` | The FTP/cPanel username |
| `CPANEL_FTP_PASSWORD` | The FTP/cPanel password |

Notes:
- Deploys into `public_html/`. To deploy into a subfolder, edit `server-dir:` in the workflow (must end with `/`).
- The frontend currently talks to Supabase directly, so the deployed site works on its own today.
  Switching it to call the Java backend is the **cutover** step (not done yet) — see below.

---

## Backend → a Java host (Docker)

A [backend/Dockerfile](backend/Dockerfile) is provided. Build and run anywhere that runs containers:

```bash
docker build -t accounting-backend ./backend
docker run -p 8080:8080 \
  -e SUPABASE_DB_URL="jdbc:postgresql://db.<project>.supabase.co:5432/postgres?sslmode=require" \
  -e SUPABASE_DB_USER="postgres" \
  -e SUPABASE_DB_PASSWORD="********" \
  -e JWT_SECRET="<32+ byte random string>" \
  -e CORS_ORIGINS="https://yourdomain.com" \
  accounting-backend
```

Good managed options: **Railway, Render, Fly.io** (point them at `backend/` with the Dockerfile),
or a **VPS** (install Docker, or a JDK + run the jar behind Apache/Nginx as a reverse proxy).

---

## Cutover (future)

Today the React app uses `src/supabase.js`. The drop-in replacement `src/api.js` (which calls the
Java backend) is built and ready. At cutover: point the app at `api.js`, set the frontend's API base
to the backend URL, remove the browser Supabase key, and run the backend's `ddl-auto=validate` against
the real database first to confirm the entity mappings.
