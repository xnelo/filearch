ALTER TABLE IF EXISTS file_tags
    ADD CONSTRAINT unique_file_tagging UNIQUE (file_id, tag_id);