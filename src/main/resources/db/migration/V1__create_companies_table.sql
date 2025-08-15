CREATE SCHEMA IF NOT EXISTS app_job_scraper;

CREATE TABLE companies
(
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255)             NOT NULL,
    website_url VARCHAR(500),
    logo_url    VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_companies_title ON companies (title);
CREATE INDEX idx_companies_website_url ON companies (website_url);