CREATE TABLE IF NOT EXISTS group_member_permissions
(
    user_id             BIGINT,
    group_id            BIGINT,
    permission_granted  SMALLINT
);

CREATE TABLE IF NOT EXISTS group_audit_log
(
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT,
    user_id         BIGINT,
    modified_at     TIMESTAMPTZ,
    modification    SMALLINT,
    pre_op_data     jsonb,
    post_op_data    jsonb
);