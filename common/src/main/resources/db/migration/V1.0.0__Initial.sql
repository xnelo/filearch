CREATE TABLE IF NOT EXISTS users
(
    id  INT PRIMARY KEY,
    external_id VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS stored_files
(
  id   INT PRIMARY KEY,
  owner_user_id INT,
  storage_type VARCHAR(3),
  storage_key VARCHAR
);

CREATE SEQUENCE IF NOT EXISTS seq_file_upload_number;