# ELMKUSOMA

ELMKUSOMA is an education management platform for schools, colleges, and universities across Africa.

## Quick Start

### Prerequisites

- **Java 17+** (JDK)
- **Maven** (or use the included `mvnw`)
- **PostgreSQL 14+**
- **Node.js 18+** (for frontend)

### 1. Install PostgreSQL

Download and install PostgreSQL from [postgresql.org](https://www.postgresql.org/download/).

During installation, remember the password you set for the `postgres` superuser.

### 2. Create the Database

Open a terminal and run:

```bash
psql -U postgres -f backend/setup-db.sql
```

Or on Windows (PowerShell):

```powershell
.\backend\setup-db.ps1
```

This creates the `elmkusoma` database. All developers use the **same database name**.

### 3. Configure Your Local Environment

```bash
cd backend
cp .env.example .env
```

Edit `.env` and set your PostgreSQL password:

```
DB_PASSWORD=your_postgres_password_here
```

All other values can stay as defaults. The `.env` file is **git-ignored** and will never be committed.

> **Note:** The app loads `.env` from `backend/.env` automatically. If that doesn't work, you can also set `DB_PASSWORD` as an environment variable in your shell or IDE.

### 4. Start the Backend

```bash
cd backend/elmkusoma-core
./mvnw spring-boot:run
```

Or on Windows:

```powershell
cd backend\elmkusoma-core
.\mvnw.cmd spring-boot:run
```

Flyway automatically applies all database migrations on first run.

### 5. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The application is now running at:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html

## Database Configuration

### Environment Variables

All developers use the **same database name** (`elmkusoma`) but can have **different passwords**.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `elmkusoma` | Database name (MUST be `elmkusoma`) |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | *(empty)* | Your local PostgreSQL password |
| `JWT_SECRET` | *(auto-generated)* | JWT signing secret |

### How Configuration Works

1. `application.yml` defines defaults (e.g., `DB_NAME=elmkusoma`)
2. Environment variables override defaults (e.g., `DB_PASSWORD=mypassword`)
3. `.env` file is loaded automatically if present (Spring Boot 3.x `config.import`)
4. The `.env` file is **never committed** to Git

### For New Developers

1. Install PostgreSQL
2. Run `setup-db.sql` to create the `elmkusoma` database
3. Copy `.env.example` to `.env`
4. Set your PostgreSQL password in `.env`
5. Start the backend — Flyway handles schema migrations

## Project Structure

```
elmkusoma/
├── backend/
│   ├── elmkusoma-core/          # Spring Boot application
│   │   ├── src/main/java/       # Java source code
│   │   ├── src/main/resources/
│   │   │   ├── application.yml  # App configuration
│   │   │   └── db/migration/    # Flyway SQL migrations
│   │   └── pom.xml              # Maven dependencies
│   ├── .env.example             # Environment variable template
│   ├── setup-db.sql             # Database creation script
│   └── setup-db.ps1             # Windows setup script
├── frontend/                    # Next.js application
│   ├── app/                     # Pages and routes
│   ├── components/              # React components
│   └── lib/                     # Utilities and API clients
└── README.md
```

## Development Rules

### Database

- **Database name:** Always `elmkusoma` — do not change
- **Migrations:** Managed by Flyway — do not edit applied migrations
- **Schema changes:** Create a new migration file (e.g., `V25__description.sql`)
- **Passwords:** Each developer uses their own via `.env` — never commit real passwords

### Backend

- Java 17, Spring Boot 3.4.x
- JPA/Hibernate with PostgreSQL
- Flyway for database migrations
- Environment variables for all secrets

### Frontend

- Next.js 16.x with React 19
- TypeScript
- Tailwind CSS
- API calls to `http://localhost:8080`

## Troubleshooting

### "Password authentication failed"

Your `.env` file has the wrong password. Set `DB_PASSWORD` to your PostgreSQL password.

### "Database does not exist"

Run `setup-db.sql` to create the `elmkusoma` database.

### "Flyway validation failed"

Do not disable Flyway validation. Instead, check that your database has all migrations applied:

```bash
psql -U postgres -d elmkusoma -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank;"
```

### "Connection refused"

Ensure PostgreSQL is running on `localhost:5432`.

## License

Proprietary — ELMKUSOMA
