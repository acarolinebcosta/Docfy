CREATE SEQUENCE document_code_sequence
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

ALTER TABLE documents
    ADD COLUMN document_code VARCHAR(20);

WITH ordered_documents AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY created_at, id) AS code_number
    FROM documents
)
UPDATE documents
SET document_code =
        'DOC-' || LPAD(ordered_documents.code_number::TEXT, 6, '0')
FROM ordered_documents
WHERE documents.id = ordered_documents.id;

DO $$
DECLARE
    existing_document_count BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO existing_document_count
    FROM documents;

    IF existing_document_count = 0 THEN
        PERFORM setval('document_code_sequence', 1, FALSE);
    ELSE
        PERFORM setval(
                'document_code_sequence',
                existing_document_count,
                TRUE
        );
    END IF;
END $$;

ALTER TABLE documents
    ALTER COLUMN document_code SET DEFAULT (
        'DOC-' || LPAD(nextval('document_code_sequence')::TEXT, 6, '0')
    ),
    ALTER COLUMN document_code SET NOT NULL;

ALTER TABLE documents
    ADD CONSTRAINT uk_documents_document_code UNIQUE (document_code);
