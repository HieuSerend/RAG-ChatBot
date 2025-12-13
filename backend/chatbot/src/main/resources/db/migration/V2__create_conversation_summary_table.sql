-- Create conversation_summary table
CREATE TABLE IF NOT EXISTS conversation_summary (
    conversation_id VARCHAR(255) PRIMARY KEY,
    summary TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
