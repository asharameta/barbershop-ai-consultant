CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store(
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);
