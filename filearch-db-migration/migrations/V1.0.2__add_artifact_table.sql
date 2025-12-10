CREATE TABLE IF NOT EXISTS artifacts
(
    id              BIGSERIAL PRIMARY KEY,
    owner_user_id   BIGINT,
    stored_files_id BIGINT,
    storage_key     VARCHAR,
    mime_type       VARCHAR(255)
);