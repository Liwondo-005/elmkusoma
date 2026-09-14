# ELMKUSOMA Education Platform

A full-stack education management system with Spring Boot backend and Next.js frontend.

## Developer Setup

### Prerequisites

- **JDK 17+** (recommended: Eclipse Adoptium JDK 21)
- **Maven 3.8+** (or use the included `mvnw`)
- **PostgreSQL 14+** (recommended: PostgreSQL 18)
- **Node.js 18+** (for frontend)

### Database Setup

All developers use the **same** database configuration:

| Setting    | Value        |
|------------|--------------|
| Database   | `elmkusoma`  |
| Host       | `localhost`  |
| Port       | `5432`       |
| Username   | `postgres`   |
| Password   | per-developer|

**Step 1: Install and start PostgreSQL**

Download from [postgresql.org](https://www.postgresql.org/download/) or use your system package manager.

**Step 2: Create the database**

```bash
psql -U postgres -c "CREATE DATABASE elmkusoma;"
```

Or use the included setup script:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File setup-db.ps1
```

**Step 3: Configure your local password**

```bash
cd backend
cp .env.example .env
```

Edit `.env` and set your local PostgreSQL password:

```
DB_PASSWORD=your_actual_password
```

> **Never commit your `.env` file.** It is already in `.gitignore`.

### Running the Backend

**Option A: Using the start script (recommended)**

```powershell
cd backend
.\start.ps1
```

This script:
- Loads your `.env` file
- Starts PostgreSQL if needed
- Builds the backend if needed
- Runs Flyway migrations automatically
- Starts the Spring Boot application on `http://localhost:8080`

**Option B: Manual**

```powershell
cd backend/elmkusoma-core
# Set environment variables (or rely on .env loaded by your IDE)
$env:DB_PASSWORD = "your_password"
mvn spring-boot:run
```

### Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`.

### How Configuration Works

```
.env.example  ──copy──>  .env  (git-ignored, local only)
                              │
                              v
application.yml  ──reads──>  ${DB_PASSWORD}  ──uses──>  PostgreSQL
```

- `application.yml` defines the schema with environment variable placeholders
- `.env` provides your local values (loaded by `start.ps1` or your IDE)
- `.env` is **never committed** to Git
- `.env.example` is committed as a template for new developers

### Flyway Migrations

Database schema is managed by Flyway. Migration files are in:

```
backend/elmkusoma-core/src/main/resources/db/migration/
```

- **Do not** modify already-applied migration files
- **Do not** create developer-specific migrations
- To add schema changes, create a new file: `V{next_number}__description.sql`
- Flyway runs automatically on application startup

### Project Structure

```
Elmukusoma/
├── backend/
│   ├── elmkusoma-core/          # Spring Boot application
│   │   ├── src/main/java/       # Java source code
│   │   ├── src/main/resources/  # Config + Flyway migrations
│   │   └── pom.xml              # Maven dependencies
│   ├── .env.example             # Environment template
│   ├── start.ps1                # Developer start script
│   └── setup-db.ps1             # Database setup script
├── frontend/                    # Next.js application
└── README.md
```

### Environment Variables

| Variable     | Default      | Description                    |
|--------------|-------------|--------------------------------|
| `DB_HOST`    | `localhost` | PostgreSQL host                |
| `DB_PORT`    | `5432`      | PostgreSQL port                |
| `DB_NAME`    | `elmkusoma` | Database name (same for all)   |
| `DB_USERNAME`| `postgres`  | Database user (same for all)   |
| `DB_PASSWORD`| (required)  | Your local PostgreSQL password |
| `JWT_SECRET` | (in .env.example) | JWT signing key           |

### Team Rules

1. **Same database name**: `elmkusoma` for all developers
2. **Same migrations**: shared in Git, applied by Flyway
3. **Different passwords**: each developer sets their own in `.env`
4. **Never commit secrets**: `.env` is always git-ignored
5. **No Docker required**: run PostgreSQL natively

### API Documentation

Once the backend is running:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`
