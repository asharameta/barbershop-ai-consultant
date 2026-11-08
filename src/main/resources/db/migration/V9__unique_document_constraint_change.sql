ALTER TABLE loaded_document DROP CONSTRAINT unique_document;

ALTER TABLE loaded_document ADD CONSTRAINT unique_document UNIQUE (filename);