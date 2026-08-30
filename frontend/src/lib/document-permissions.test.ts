import { describe, expect, it } from "vitest";

import {
  availableWorkflowActions,
  canEditDocument,
  canViewDocumentAudit,
} from "@/lib/document-permissions";
import type {
  AuthUser,
  Role,
} from "@/types/auth";
import type {
  Document,
  DocumentStatus,
} from "@/types/document";

function user(role: Role, id = "current-user"): AuthUser {
  return {
    id,
    email: `${role.toLowerCase()}@docfy.local`,
    role,
    expiresAt: Date.now() + 60_000,
  };
}

function document(
  status: DocumentStatus,
  createdBy = "current-user",
): Document {
  return {
    id: "document-id",
    title: "Document",
    description: null,
    status,
    createdBy,
    createdAt: "2026-08-30T12:00:00Z",
    updatedAt: "2026-08-30T12:00:00Z",
  };
}

describe("document role and status UX", () => {
  it("allows a collaborator to edit and submit only their own draft", () => {
    const collaborator = user("COLLABORATOR");

    expect(canEditDocument(document("DRAFT"), collaborator)).toBe(true);
    expect(
      availableWorkflowActions(document("DRAFT"), collaborator),
    ).toEqual(["submit"]);

    expect(
      canEditDocument(document("DRAFT", "another-user"), collaborator),
    ).toBe(false);
    expect(
      availableWorkflowActions(
        document("DRAFT", "another-user"),
        collaborator,
      ),
    ).toEqual([]);
  });

  it("shows review and archive actions only to manager or admin", () => {
    for (const role of ["MANAGER", "ADMIN"] as const) {
      const privilegedUser = user(role);

      expect(
        availableWorkflowActions(
          document("IN_REVIEW", "another-user"),
          privilegedUser,
        ),
      ).toEqual(["approve", "reject"]);
      expect(
        availableWorkflowActions(
          document("APPROVED", "another-user"),
          privilegedUser,
        ),
      ).toEqual(["archive"]);
      expect(canViewDocumentAudit(privilegedUser)).toBe(true);
    }

    const collaborator = user("COLLABORATOR");
    expect(
      availableWorkflowActions(document("IN_REVIEW"), collaborator),
    ).toEqual([]);
    expect(
      availableWorkflowActions(document("APPROVED"), collaborator),
    ).toEqual([]);
    expect(canViewDocumentAudit(collaborator)).toBe(false);
  });

  it("does not offer metadata editing outside draft status", () => {
    for (const role of [
      "ADMIN",
      "MANAGER",
      "COLLABORATOR",
    ] as const) {
      const currentUser = user(role);

      expect(canEditDocument(document("IN_REVIEW"), currentUser)).toBe(false);
      expect(canEditDocument(document("APPROVED"), currentUser)).toBe(false);
      expect(canEditDocument(document("ARCHIVED"), currentUser)).toBe(false);
    }
  });
});
