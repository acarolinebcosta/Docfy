export type DocumentStatus =
  | "DRAFT"
  | "IN_REVIEW"
  | "APPROVED"
  | "ARCHIVED";

export interface Category {
  id: string;
  name: string;
}

export interface Document {
  id: string;
  documentCode: string;
  title: string;
  description: string | null;
  status: DocumentStatus;
  category: Category;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentPage {
  items: Document[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateDocumentInput {
  title: string;
  description: string | null;
  categoryId: string;
}

export interface UpdateDocumentInput {
  title?: string;
  description?: string | null;
  categoryId?: string;
}

export type DocumentWorkflowAction =
  | "submit"
  | "approve"
  | "reject"
  | "archive";

export type DocumentAuditAction =
  | "DOCUMENT_SUBMITTED"
  | "DOCUMENT_APPROVED"
  | "DOCUMENT_REJECTED"
  | "DOCUMENT_ARCHIVED";

export interface DocumentAuditEvent {
  id: string;
  documentId: string;
  actorId: string;
  action: DocumentAuditAction;
  previousStatus: DocumentStatus;
  newStatus: DocumentStatus;
  occurredAt: string;
  correlationId: string | null;
}

export interface DocumentFile {
  id: string;
  documentId: string;
  originalFilename: string;
  contentType: string;
  size: number;
  uploadedBy: string;
  uploadedAt: string;
}
