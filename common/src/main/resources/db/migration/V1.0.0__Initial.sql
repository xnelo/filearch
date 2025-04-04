CREATE TABLE IF NOT EXISTS test_table
(
  id   INT,
  name VARCHAR(20)
);
INSERT INTO test_table(id, name)
VALUES (1, 'QUARKED');

CREATE SEQUENCE IF NOT EXISTS seq_file_upload_number;