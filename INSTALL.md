# Installation Guide

## Prerequisites

- Docker and Docker Compose
- Git

## Installation

### Clone Repository
```bash
git clone <repository-url>
cd job-scraper
```

### Start Application
```bash
docker-compose up --build -d
```

### Verify Installation
```bash
curl http://localhost:8080/api/jobs/functions
```

Expected response: list of available job functions.

## Testing

### Scrape Jobs
```bash
curl -X POST "http://localhost:8080/api/jobs/scrape?jobFunction=Software Engineering"
```

### View Results
```bash
curl http://localhost:8080/api/jobs
```

### Export Database
```bash
curl http://localhost:8080/api/jobs/export > jobs.sql
```

## Configuration

### Database Settings
Edit `docker-compose.yml` environment variables:
- POSTGRES_DB
- POSTGRES_USER  
- POSTGRES_PASSWORD

### Application Settings
Edit `src/main/resources/application.yml`:
- Server port
- Database connection
- Timeout settings

## Troubleshooting

### Port Conflicts
If port 8080 is in use, change in `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Use 8081 instead
```

### Database Issues
Reset database:
```bash
docker-compose down -v
docker-compose up --build -d
```

### Memory Issues
Increase Docker memory limit to 4GB minimum.

## Local Development

### Requirements
- Java 17
- Maven 3.6+
- PostgreSQL 13+

### Setup
```bash
mvn clean install
export DB_HOST=localhost
./mvnw spring-boot:run
```

### Database Setup
```sql
CREATE DATABASE js_database;
CREATE USER js_user WITH PASSWORD 'js_password';
GRANT ALL PRIVILEGES ON DATABASE js_database TO js_user;
```