CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kb_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS kb_embeddings_idx
ON kb_embeddings USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS kb_embeddings_metadata_idx
ON kb_embeddings USING gin (metadata jsonb_path_ops);

CREATE TABLE IF NOT EXISTS chat_memory_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS chat_memory_embedding_idx
ON chat_memory_embeddings USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS chat_memory_metadata_idx
ON chat_memory_embeddings USING gin (metadata jsonb_path_ops);

SELECT
    'pgvector extension installed' as status,
    extversion as version
FROM pg_extension
WHERE extname = 'vector';

SELECT COUNT(*) as total_kb_embeddings FROM kb_embeddings;
SELECT COUNT(*) as total_chat_memory_embeddings FROM chat_memory_embeddings;