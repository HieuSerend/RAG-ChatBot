CREATE OR REPLACE VIEW public.knowledge_embedding_view AS
SELECT e.id,
    e.document AS content,
    e.cmetadata AS metadata,
    e.embedding
FROM langchain_pg_embedding e
    JOIN langchain_pg_collection c ON e.collection_id = c.uuid
WHERE c.name = 'gemini_knowledge_base';
CREATE OR REPLACE VIEW public.case_studies_embedding_view AS
SELECT e.id,
    e.document AS content,
    e.cmetadata AS metadata,
    e.embedding
FROM langchain_pg_embedding e
    JOIN langchain_pg_collection c ON e.collection_id = c.uuid
WHERE c.name = 'advisory_case_studies';
CREATE INDEX idx_pg_embedding_collection_id ON langchain_pg_embedding (collection_id);
CREATE UNIQUE INDEX idx_pg_collection_uuid ON langchain_pg_collection (uuid);
CREATE INDEX IF NOT EXISTS langchain_pg_embedding_idx ON langchain_pg_embedding USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS langchain_pg_embedding_metadata_idx ON langchain_pg_embedding USING gin (metadata jsonb_path_ops);
CREATE TABLE IF NOT EXISTS chat_memory_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS chat_memory_embedding_idx ON chat_memory_embedding USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS chat_memory_metadata_idx ON chat_memory_embedding USING gin (metadata jsonb_path_ops);
SELECT 'pgvector extension installed' as status,
    extversion as version
FROM pg_extension
WHERE extname = 'vector';
SELECT COUNT(*) as total_knowledge_embeddings
FROM knowledge_embedding_view;
SELECT COUNT(*) as total_case_studies_embeddings
from case_studies_embedding_view;
SELECT COUNT(*) as total_chat_memory_embeddings
FROM chat_memory_embedding;