import { apiBlobRequest, apiRequest } from "@/api/client";
import type {
  Category,
  CreateDocumentInput,
  Document,
  DocumentAuditEvent,
  DocumentFile,
  DocumentPage,
  DocumentStatus,
  DocumentWorkflowAction,
  UpdateDocumentInput,
} from "@/types/document";

export function listCategories({
  accessToken,
  signal,
}: DocumentRequestOptions): Promise<Category[]> {
  return apiRequest<Category[]>("/api/v1/categories", {
    accessToken,
    signal,
  });
}

export interface ListDocumentsOptions {
  accessToken: string;
  page?: number;
  size?: number;
  search?: string;
  categoryId?: string;
  status?: DocumentStatus;
  signal?: AbortSignal;
}

export function listDocuments({
  accessToken,
  page = 0,
  size = 20,
  search,
  categoryId,
  status,
  signal,
}: ListDocumentsOptions): Promise<DocumentPage> {
  const parameters = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (search?.trim()) {
    parameters.set("search", search.trim());
  }

  if (categoryId) {
    parameters.set("categoryId", categoryId);
  }

  if (status) {
    parameters.set("status", status);
  }

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

export function listDocumentFiles({
  accessToken,
  documentId,
  signal,
}: GetDocumentOptions): Promise<DocumentFile[]> {
  return apiRequest<DocumentFile[]>(
    `/api/v1/documents/${encodeURIComponent(documentId)}/files`,
    {
      accessToken,
      signal,
    },
  );
}

interface UploadDocumentFileOptions extends GetDocumentOptions {
  file: File;
}

export function uploadDocumentFile({
  accessToken,
  documentId,
  file,
  signal,
}: UploadDocumentFileOptions): Promise<DocumentFile> {
  const body = new FormData();
  body.append("file", file);

  return apiRequest<DocumentFile>(
    `/api/v1/documents/${encodeURIComponent(documentId)}/files`,
    {
      method: "POST",
      accessToken,
      body,
      signal,
    },
  );
}

interface DownloadDocumentFileOptions extends GetDocumentOptions {
  fileId: string;
}

export function downloadDocumentFile({
  accessToken,
  documentId,
  fileId,
  signal,
}: DownloadDocumentFileOptions): Promise<Blob> {
  return apiBlobRequest(
    `/api/v1/documents/${encodeURIComponent(documentId)}/files/${encodeURIComponent(fileId)}`,
    {
      accessToken,
      signal,
    },
  );
}
