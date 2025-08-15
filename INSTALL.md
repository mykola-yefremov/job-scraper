# Installation & Setup

## Requirements
- Java 17 or higher
- Maven 3+
- Docker & Docker Compose

## Configuration
Database connection settings can be configured via environment variables in `application.yml`:
- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `5432`)
- `DB_NAME` (default: `js_database`)
- `DB_USERNAME` (default: `js_user`)
- `DB_PASSWORD` (default: `js_password`)
- `DB_APP_NAME` (default: `app_job_scraper`)
- `DB_SCHEMA` (default: `app_job_scraper`)

## Running the Project
Start PostgreSQL:
   ```bash
   docker-compose up -d
