# ELMKUSOMA Education Platform

A full-stack education management system with Spring Boot backend and Next.js frontend.

## Quick Start (New Developer)

Get the backend running in 5 steps:

```bash
# 1. Clone the repository
git clone https://github.com/Liwondo-005/elmkusoma.git
cd Elmukusoma

# 2. Install PostgreSQL (14+) and create the database
psql -U postgres -c "CREATE DATABASE elmkusoma;"

# 3. Create your local .env file with YOUR password
cd backend
cp .env.example .env
# Edit .env and set DB_PASSWORD=your_postgres_password

# 4. Start the backend
.\start.ps1

# 5. Open http://localhost:8080 — Flyway migrates automatically
```

That's it. **Do NOT edit `application.yml`.** Your password lives only in `.env`.

---

## Full Setup Guide

### Prerequisites

- **JDK 21** — [Eclipse Adoptium](https://adoptium.net/) (recommended)
- **Maven 3.8+** — [maven.apache.org](https://maven.apache.org/)
- **PostgreSQL 14+** — [postgresql.org](https://www.postgresql.org/download/) (recommended: 18)
- **Node.js 18+** — for frontend only

### Step 1 — Install and Start PostgreSQL

1. Download and install PostgreSQL from [postgresql.org](https://www.postgresql.org/download/)
2. During installation, set a password for the `postgres` user (you'll need this later)
3. Make sure PostgreSQL is running:
   - **Windows:** Check Services → `postgresql-x64-*` should be "Running"
   - **Linux/Mac:** `sudo systemctl status postgresql`

### Step 2 — Create the Database

Every developer uses the **same** database name: `elmkusoma`

```bash
psql -U postgres -c "CREATE DATABASE elmkusoma;"
```

Or use the setup script:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File setup-db.ps1
```

### Step 3 — Configure Your Local Password

```bash
cd backend
cp .env.example .env
```

Open `.env` and set **only** your PostgreSQL password:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=elmkusoma
DB_USERNAME=postgres
DB_PASSWORD=YOUR_POSTGRES_PASSWORD_HERE
```

> **Never commit your `.env` file.** It is already in `.gitignore`.

### Step 4 — Run the Backend

**Option A: Start script (recommended)**

```powershell
cd backend
.\start.ps1
```

**Option B: Maven directly**

```powershell
cd backend/elmkusoma-core
mvn spring-boot:run
```

**Option C: After building JAR**

```powershell
cd backend
java -jar elmkusoma-core\target\elmkusoma-core-0.1.0-SNAPSHOT.jar
```

All three options work because:
- `start.ps1` loads `.env` into environment variables before starting
- The application automatically loads `.env` from `backend/` on startup (built-in)

**The backend starts on `http://localhost:8080`**

### Step 5 — Verify It Works

- Backend health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`

### Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`.

---

## How Configuration Works

```
.env.example  ──copy──>  .env  (git-ignored, local only)
                               │
                               v
  ElmkusomaCoreApplication.main()  ──loads──>  System.setProperty()
                               │
                               v
  application.yml  ──reads──>  ${DB_PASSWORD}  ──uses──>  PostgreSQL
```

1. `.env.example` is committed as a template for new developers
2. Each developer copies it to `.env` and sets their own password
3. The application automatically reads `.env` on startup (no external dependencies)
4. `application.yml` uses `${DB_PASSWORD}` — Spring Boot resolves it from system properties
5. Flyway uses the **same** database connection — no separate credentials
6. `.env` is **never committed** to Git

### Environment Variables

| Variable     | Default      | Description                    |
|--------------|-------------|--------------------------------|
| `DB_HOST`    | `localhost` | PostgreSQL host                |
| `DB_PORT`    | `5432`      | PostgreSQL port                |
| `DB_NAME`    | `elmkusoma` | Database name (same for all)   |
| `DB_USERNAME`| `postgres`  | Database user (same for all)   |
| `DB_PASSWORD`| *(required)*| Your local PostgreSQL password |
| `JWT_SECRET` | (auto-set)  | JWT signing key                |

---

## Database & Migrations

### Flyway

Database schema is managed by Flyway. Migration files are in:

```
backend/elmkusoma-core/src/main/resources/db/migration/
```

- Flyway runs **automatically** on application startup
- All developers get the same schema from shared migration files
- **Do not** modify already-applied migration files
- **Do not** create developer-specific migrations
- To add schema changes, create: `V{next_number}__description.sql`

### Team Rules

| Rule | Detail |
|------|--------|
| Same database name | `elmkusoma` for all developers |
| Same migrations | Shared in Git, applied by Flyway |
| Different passwords | Each developer sets their own in `.env` |
| Never commit secrets | `.env` is always git-ignored |
| No Docker required | Run PostgreSQL natively |
| Never edit `application.yml` | Password is in `.env` only |

---

## Troubleshooting

### "password authentication failed for user 'postgres'"

Your `.env` password does not match your PostgreSQL password.

**Fix:**
```bash
# Test your password works
psql -U postgres -d elmkusoma

# If it fails, reset your PostgreSQL password:
psql -U postgres -c "ALTER USER postgres PASSWORD 'new_password';"

# Then update your .env file with the correct password
```

### "Database 'elmkusoma' does not exist"

Create the database:
```bash
psql -U postgres -c "CREATE DATABASE elmkusoma;"
```

### "Relation 'flyway_schema_history' does not exist"

This means Flyway hasn't run yet. Just start the backend — Flyway runs automatically on first startup.

### "Permission denied for table ..."

Your PostgreSQL user doesn't have permissions. Grant them:
```sql
psql -U postgres -d elmkusoma -c "GRANT ALL ON SCHEMA public TO postgres;"
```

### Backend starts but Redis warnings appear

These are **harmless** informational messages:
```
Spring Data Redis - Could not safely identify store assignment...
```
Redis is optional. The backend works without it. You can ignore these warnings.

### "Port 8080 already in use"

Another process is using port 8080:
```powershell
# Windows: find and kill the process
netstat -ano | findstr :8080
taskkill /PID <process_id> /F
```

---

## Project Structure

```
Elmukusoma/
├── backend/
│   ├── elmkusoma-core/          # Spring Boot application
│   │   ├── src/main/java/       # Java source code
│   │   ├── src/main/resources/  # Config + Flyway migrations
│   │   └── pom.xml              # Maven dependencies
│   ├── .env.example             # Environment template (committed)
│   ├── .env                     # Your local password (git-ignored)
│   ├── start.ps1                # Developer start script
│   └── setup-db.ps1             # Database setup script
├── frontend/                    # Next.js application
└── README.md
```
