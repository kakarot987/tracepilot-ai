
CREATE TABLE rag_documents (
    document_id VARCHAR(40) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    source_path VARCHAR(500) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE rag_chunks (
    chunk_id VARCHAR(100) PRIMARY KEY,
    document_id VARCHAR(40) NOT NULL
        REFERENCES rag_documents(document_id)
        ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    content TEXT NOT NULL,
    embedding vector(768) NOT NULL,
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_rag_documents_service
    ON rag_documents(service_name);

CREATE INDEX idx_rag_documents_type
    ON rag_documents(document_type);

CREATE INDEX idx_rag_chunks_document
    ON rag_chunks(document_id);
