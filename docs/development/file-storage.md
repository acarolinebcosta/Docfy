# Docfy File Storage

## Purpose

Docfy stores attachment metadata in PostgreSQL and binary content through a
`FileStorage` abstraction. The current development adapter uses the local
filesystem; external object storage is outside the MVP.

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `DOCFY_FILE_STORAGE_PATH` | `./data/files` | Root for local binary content |
| `DOCFY_FILE_MAX_SIZE` | `10MB` | Application upload limit |
| `DOCFY_FILE_REQUEST_MAX_SIZE` | `11MB` | Multipart request/container limit |

Relative storage paths are resolved from the backend process working
directory. `backend/data/` is ignored by Git. Production-like environments
must provide an appropriate persistent path and filesystem permissions.

## API

All routes require a bearer JWT:

| Method | Route | Behavior |
|---|---|---|
| `POST` | `/api/v1/documents/{documentId}/files` | Upload multipart part named `file` |
| `GET` | `/api/v1/documents/{documentId}/files` | List attachment metadata |
| `GET` | `/api/v1/documents/{documentId}/files/{fileId}` | Download binary content |

Upload uses the existing draft edit policy. Listing and download use the
document visibility policy. Missing and concealed resources have the same safe
`404 Document not found` behavior.

## Validation

Supported formats:

| Extension | Declared content type | Content check |
|---|---|---|
| `.pdf` | `application/pdf` | `%PDF-` prefix |
| `.txt` | `text/plain` | valid UTF-8, no NUL bytes |
| `.png` | `image/png` | PNG signature |
| `.jpg`, `.jpeg` | `image/jpeg` | JPEG signature prefix |

Files must be non-empty and no larger than the configured application limit.
The validator checks extension, declared content type and the content rule
above. This is not antivirus scanning or general-purpose MIME detection.

Client filenames never become physical paths. The backend keeps a sanitized
basename as metadata and creates a random UUID storage key with the validated
extension. The local adapter normalizes that key under the configured root and
uses create-new writes to prevent traversal, collision and overwrite.

## Database and storage consistency

Upload follows this order:

```text
validate authorization and content
        ↓
write binary through FileStorage
        ↓
persist metadata in PostgreSQL transaction
        ↓
transaction commits
```

If storage fails, no metadata is persisted. If metadata persistence or the
transaction fails after the write, transaction synchronization removes the
stored object. A compensation-delete failure is logged for operational cleanup
without logging file content, credentials or tokens.

Because PostgreSQL and a filesystem do not share an ACID transaction, a host
failure between these operations remains a reliability concern for future
object-storage/outbox work. The implemented compensation covers known
application failure paths.

## Development seed

The opt-in `dev` profile provisions three small synthetic text attachments.
They are classpath resources, contain no personal or corporate data and are
uploaded through the same application service and validation rules as normal
files. Restarting the seed does not duplicate attachment metadata or content.
