CREATE TABLE IF NOT EXISTS tags
(
    id              BIGSERIAL PRIMARY KEY,
    owner_user_id   BIGINT,
    tag_name        BYTEA  -- ENCRYPTED
);

CREATE TABLE IF NOT EXISTS file_tags
(
    file_id     BIGINT,
    tag_id      BIGINT
);
