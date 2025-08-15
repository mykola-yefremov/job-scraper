CREATE TABLE jobs
(
    id               BIGSERIAL PRIMARY KEY,
    position_name    VARCHAR(255),
    job_page_url     VARCHAR(500) UNIQUE,
    labor_function   VARCHAR(255),
    location         VARCHAR(255),
    posted_date_unix BIGINT,
    description      TEXT,
    status           VARCHAR(50) DEFAULT 'PENDING',
    company_id       BIGINT REFERENCES companies (id),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_jobs_job_page_url ON jobs (job_page_url);
CREATE INDEX idx_jobs_labor_function ON jobs (labor_function);
CREATE INDEX idx_jobs_posted_date_unix ON jobs (posted_date_unix);
CREATE INDEX idx_jobs_location ON jobs (location);
CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_company_id ON jobs (company_id);