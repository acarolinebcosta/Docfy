export type DocumentStatus =
  | "DRAFT"
  | "IN_REVIEW"
  | "APPROVED"
  | "ARCHIVED";

export interface Document {
  id: string;
  title: string;
  description: string | null;
  status: DocumentStatus;
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
}

export interface UpdateDocumentInput {
  title?: string;
  description?: string | null;
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
