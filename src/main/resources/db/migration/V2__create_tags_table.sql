CREATE TABLE tags
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE UNIQUE INDEX idx_tags_name ON tags (name);