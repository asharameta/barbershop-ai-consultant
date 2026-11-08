CREATE TABLE loaded_document(
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_hash VARCHAR(255) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    chunk_count INTEGER NOT NULL,
    loaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT unique_document UNIQUE (filename, content_hash)
);

CREATE INDEX idx_loaded_documents_filename
    ON loaded_document(filename);