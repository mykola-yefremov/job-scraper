CREATE TABLE tags
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX idx_tags_name ON tags (name);