# Job Scraper

Spring Boot application for scraping job listings from TechStars portfolio.

## Features

- Web scraping from jobs.techstars.com
- PostgreSQL database storage
- REST API endpoints
- Docker containerization
- SQL export functionality

## Technology Stack

- Java 17
- Spring Boot 3.x
- PostgreSQL
- JSoup
- Maven
- Docker

## API Endpoints

### Scrape Jobs
```
POST /api/jobs/scrape?jobFunction={function}
```

### Get All Jobs
```
GET /api/jobs
```

### Get Jobs by Function
```
GET /api/jobs/function/{function}
```

### Available Functions
```
GET /api/jobs/functions
```

### Export SQL
```
GET /api/jobs/export
```

## Quick Start

```bash
docker-compose up --build
```

Application will be available at http://localhost:8080

## Usage Example

```bash
curl -X POST "http://localhost:8080/api/jobs/scrape?jobFunction=Software Engineering"
curl "http://localhost:8080/api/jobs"
curl "http://localhost:8080/api/jobs/export" > jobs_dump.sql
```

## Database Schema

- **companies**: id, title, website_url, logo_url
- **jobs**: id, position_name, job_page_url, labor_function, location, posted_date_unix, description, status, company_id
- **tags**: id, name
- **job_tags**: job_id, tag_id

## Environment Variables

- DB_HOST (default: localhost)
- DB_PORT (default: 5432)
- DB_NAME (default: js_database)
- DB_USERNAME (default: js_user)
- DB_PASSWORD (default: js_password)