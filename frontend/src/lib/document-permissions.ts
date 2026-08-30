import type { AuthUser } from "@/types/auth";
import type {
  Document,
  DocumentWorkflowAction,
} from "@/types/document";

function isManagerOrAdmin(user: AuthUser): boolean {
  return user.role === "ADMIN" || user.role === "MANAGER";
}

export function canEditDocument(
  document: Document,
  user: AuthUser,
): boolean {
  if (document.status !== "DRAFT") {
    return false;
  }

  return isManagerOrAdmin(user) || document.createdBy === user.id;
}

export function availableWorkflowActions(
  document: Document,
  user: AuthUser,
): DocumentWorkflowAction[] {
  if (document.status === "DRAFT") {
    if (isManagerOrAdmin(user) || document.createdBy === user.id) {
      return ["submit"];
    }

    return [];
  }

  if (document.status === "IN_REVIEW" && isManagerOrAdmin(user)) {
    return ["approve", "reject"];
  }

  if (document.status === "APPROVED" && isManagerOrAdmin(user)) {
    return ["archive"];
  }

  return [];
}

export function canViewDocumentAudit(user: AuthUser): boolean {
  return isManagerOrAdmin(user);
}
