import { render } from "@testing-library/react";
import {
  Link,
  MemoryRouter,
  Route,
  Routes,
} from "react-router-dom";

import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { DocumentDetailPage } from "@/pages/DocumentDetailPage";
import { createTestToken } from "@/test/token";
import type { Role } from "@/types/auth";
import type {
  Document,
  DocumentAuditEvent,
} from "@/types/document";

export const OWNER_ID = "11111111-1111-1111-1111-111111111111";
export const OTHER_USER_ID = "22222222-2222-2222-2222-222222222222";
export const DOCUMENT_A_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
export const DOCUMENT_B_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

export function documentFixture(
  overrides: Partial<Document> = {},
): Document {
  return {
    id: DOCUMENT_A_ID,
    title: "Quality Policy",
    description: "Policy description",
    status: "DRAFT",
    createdBy: OWNER_ID,
    createdAt: "2026-08-30T12:00:00Z",
    updatedAt: "2026-08-30T13:00:00Z",
    ...overrides,
  };
}

export function auditEventFixture(
  overrides: Partial<DocumentAuditEvent> = {},
): DocumentAuditEvent {
  return {
    id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
    documentId: DOCUMENT_A_ID,
    actorId: OTHER_USER_ID,
    action: "DOCUMENT_SUBMITTED",
    previousStatus: "DRAFT",
    newStatus: "IN_REVIEW",
    occurredAt: "2026-08-30T14:00:00Z",
    correlationId: "audit-correlation",
    ...overrides,
  };
}

export function renderDocumentDetail({
  role = "COLLABORATOR",
  userId = OWNER_ID,
  documentId = DOCUMENT_A_ID,
}: {
  role?: Role;
  userId?: string;
  documentId?: string;
} = {}) {
  sessionStorage.setItem(
    "docfy.accessToken",
    createTestToken({ id: userId, role }),
  );

  return render(
    <MemoryRouter initialEntries={[`/documents/${documentId}`]}>
      <AuthProvider>
        <Link to={`/documents/${DOCUMENT_B_ID}`}>
          Abrir documento B
        </Link>
        <Routes>
          <Route path="/login" element={<p>Acesse o Docfy</p>} />
          <Route element={<ProtectedRoute />}>
            <Route
              path="/documents/:id"
              element={<DocumentDetailPage />}
            />
            <Route path="/documents" element={<p>Documentos</p>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}
