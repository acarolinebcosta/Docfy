CREATE TABLE document_files (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uq_document_files_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_document_files_size CHECK (size_bytes > 0),
    CONSTRAINT fk_document_files_document
        FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT fk_document_files_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE INDEX idx_document_files_document
    ON document_files(document_id, uploaded_at, id);

