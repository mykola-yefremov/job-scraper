CREATE TABLE job_tags
(
    job_id BIGINT REFERENCES jobs (id),
    tag_id BIGINT REFERENCES tags (id),
    PRIMARY KEY (job_id, tag_id)
);

CREATE INDEX idx_job_tags_job_id ON job_tags (job_id);
CREATE INDEX idx_job_tags_tag_id ON job_tags (tag_id);