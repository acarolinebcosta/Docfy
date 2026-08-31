CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,

    CONSTRAINT uk_categories_name UNIQUE (name)
);

INSERT INTO categories (id, name)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'Meeting Minutes'),
    ('11111111-0000-0000-0000-000000000002', 'Certificate'),
    ('11111111-0000-0000-0000-000000000003', 'Contract'),
    ('11111111-0000-0000-0000-000000000004', 'Notice'),
    ('11111111-0000-0000-0000-000000000005', 'Regulation'),
    ('11111111-0000-0000-0000-000000000006', 'Official Letter'),
    ('11111111-0000-0000-0000-000000000007', 'Other');

ALTER TABLE documents
    ADD COLUMN category_id UUID;

UPDATE documents
SET category_id = '11111111-0000-0000-0000-000000000007';

ALTER TABLE documents
    ALTER COLUMN category_id SET NOT NULL,
    ADD CONSTRAINT fk_documents_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id);

CREATE INDEX idx_documents_category
    ON documents(category_id);
