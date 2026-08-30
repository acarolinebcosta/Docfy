import { apiRequest } from "@/api/client";
import type {
  CreateDocumentInput,
  Document,
  DocumentAuditEvent,
  DocumentPage,
  DocumentWorkflowAction,
  UpdateDocumentInput,
} from "@/types/document";

export interface ListDocumentsOptions {
  accessToken: string;
  page?: number;
  size?: number;
  signal?: AbortSignal;
}

export function listDocuments({
  accessToken,
  page = 0,
  size = 20,
  signal,
}: ListDocumentsOptions): Promise<DocumentPage> {
  const parameters = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  return apiRequest<DocumentPage>(
    `/api/v1/documents?${parameters.toString()}`,
    {
      accessToken,
      signal,
    },
  );
}

interface DocumentRequestOptions {
  accessToken: string;
  signal?: AbortSignal;
}

interface CreateDocumentOptions extends DocumentRequestOptions {
  document: CreateDocumentInput;
}

export function createDocument({
  accessToken,
  document,
  signal,
}: CreateDocumentOptions): Promise<Document> {
  return apiRequest<Document>("/api/v1/documents", {
    method: "POST",
    accessToken,
    body: JSON.stringify(document),
    signal,
  });
}

interface GetDocumentOptions extends DocumentRequestOptions {
  documentId: string;
}

export function getDocument({
  accessToken,
  documentId,
  signal,
}: GetDocumentOptions): Promise<Document> {
  return apiRequest<Document>(
    `/api/v1/documents/${encodeURIComponent(documentId)}`,
    {
      accessToken,
      signal,
    },
  );
}

interface UpdateDocumentOptions extends GetDocumentOptions {
  changes: UpdateDocumentInput;
}

export function updateDocument({
  accessToken,
  documentId,
  changes,
  signal,
}: UpdateDocumentOptions): Promise<Document> {
  return apiRequest<Document>(
    `/api/v1/documents/${encodeURIComponent(documentId)}`,
    {
      method: "PATCH",
      accessToken,
      body: JSON.stringify(changes),
      signal,
    },
  );
}

interface WorkflowDocumentOptions extends GetDocumentOptions {
  action: DocumentWorkflowAction;
}

export function runDocumentWorkflow({
  accessToken,
  documentId,
  action,
  signal,
}: WorkflowDocumentOptions): Promise<Document> {
  return apiRequest<Document>(
    `/api/v1/documents/${encodeURIComponent(documentId)}/${action}`,
    {
      method: "POST",
      accessToken,
      signal,
    },
  );
}

export function getDocumentAudit({
  accessToken,
  documentId,
  signal,
}: GetDocumentOptions): Promise<DocumentAuditEvent[]> {
  return apiRequest<DocumentAuditEvent[]>(
    `/api/v1/documents/${encodeURIComponent(documentId)}/audit`,
    {
      accessToken,
      signal,
    },
  );
}
