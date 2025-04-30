CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- function from https://stackoverflow.com/a/13599525
CREATE OR REPLACE FUNCTION pgp_sym_decrypt_null_on_err(data bytea, psw text) RETURNS text AS $$
BEGIN
  RETURN pgp_sym_decrypt(data, psw);
EXCEPTION
  WHEN external_routine_invocation_exception THEN
    RAISE DEBUG USING
       MESSAGE = format('Decryption failed: SQLSTATE %s, Msg: %s',
                        SQLSTATE,SQLERRM),
       HINT = 'pgp_sym_encrypt(...) failed; check your key',
       ERRCODE = 'external_routine_invocation_exception';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS users
(
    id  BIGSERIAL PRIMARY KEY,
    username BYTEA, -- ENCRYPTED
    first_name BYTEA, -- ENCRYPTED
    last_name BYTEA, -- ENCRYPTED
    email BYTEA, -- ENCRYPTED
    external_id VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS stored_files
(
  id   BIGSERIAL PRIMARY KEY,
  owner_user_id BIGINT,
  storage_type VARCHAR(3),
  storage_key VARCHAR,
  original_filename BYTEA -- ENCRYPTED
);

CREATE SEQUENCE IF NOT EXISTS seq_file_upload_number;