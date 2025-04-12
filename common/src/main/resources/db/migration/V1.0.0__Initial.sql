CREATE TABLE IF NOT EXISTS users
(
    id  INT PRIMARY KEY,
    username VARCHAR(64),
    first_name VARCHAR(96),
    last_name VARCHAR(96),
    email VARCHAR(255),
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