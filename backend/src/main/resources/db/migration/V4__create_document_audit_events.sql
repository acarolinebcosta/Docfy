CREATE TABLE document_audit_events (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    previous_status VARCHAR(30) NOT NULL,
    new_status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(36),

    CONSTRAINT fk_document_audit_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_document_audit_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id),

    CONSTRAINT chk_document_audit_action
        CHECK (
            action IN (
                'DOCUMENT_SUBMITTED',
                'DOCUMENT_APPROVED',
                'DOCUMENT_REJECTED',
                'DOCUMENT_ARCHIVED'
            )
        ),

    CONSTRAINT chk_document_audit_previous_status
        CHECK (
            previous_status IN (
                'DRAFT',
                'IN_REVIEW',
                'APPROVED',
                'ARCHIVED'
            )
        ),

    CONSTRAINT chk_document_audit_new_status
        CHECK (
            new_status IN (
                'DRAFT',
                'IN_REVIEW',
                'APPROVED',
                'ARCHIVED'
            )
        ),

    CONSTRAINT chk_document_audit_status_changed
        CHECK (previous_status <> new_status)
);

CREATE INDEX idx_document_audit_document
    ON document_audit_events(document_id);

CREATE INDEX idx_document_audit_document_occurred_at
    ON document_audit_events(document_id, occurred_at, id);

CREATE INDEX idx_document_audit_actor
    ON document_audit_events(actor_id);