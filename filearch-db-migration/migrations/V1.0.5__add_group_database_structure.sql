CREATE TABLE IF NOT EXISTS groups
(
    id              BIGSERIAL PRIMARY KEY,
    owner_user_id   BIGINT,
    group_name      BYTEA  -- ENCRYPTED
);

CREATE TABLE IF NOT EXISTS group_members
(
    user_id     BIGINT,
    group_id    BIGINT,
    accepted    BOOLEAN
);

CREATE TABLE IF NOT EXISTS group_items
(
    item_id     BIGINT,
    item_type   SMALLINT,
    group_id    BIGINT
);