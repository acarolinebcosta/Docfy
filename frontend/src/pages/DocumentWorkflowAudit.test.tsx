import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, delay, http } from "msw";
import {
  describe,
  expect,
  it,
  vi,
} from "vitest";

import { formatDateTime } from "@/lib/date";
import { server } from "@/test/server";
import {
  OTHER_USER_ID,
  OWNER_ID,
  auditEventFixture,
  documentFixture,
  renderDocumentDetail,
} from "@/test/document-detail";
import type { Role } from "@/types/auth";
import type {
  Document,
  DocumentAuditEvent,
  DocumentStatus,
  DocumentWorkflowAction,
} from "@/types/document";

function useDocumentAndAudit(
  document: Document,
  events: DocumentAuditEvent[] = [],
) {
  server.use(
    http.get("*/api/v1/documents/:id", () =>
      HttpResponse.json(document),
    ),
    http.get("*/api/v1/documents/:id/audit", () =>
      HttpResponse.json(events),
    ),
  );
}

const workflowCases: readonly [
  DocumentWorkflowAction,
  DocumentStatus,
  DocumentStatus,
  Role,
  string,
  string,
][] = [
  [
    "submit",
    "DRAFT",
    "IN_REVIEW",
    "COLLABORATOR",
    OWNER_ID,
    "Enviar para revisão",
  ],
  [
    "approve",
    "IN_REVIEW",
    "APPROVED",
    "MANAGER",
    OTHER_USER_ID,
    "Aprovar",
  ],
  [
    "reject",
    "IN_REVIEW",
    "DRAFT",
    "ADMIN",
    OTHER_USER_ID,
    "Rejeitar",
  ],
  [
    "archive",
    "APPROVED",
    "ARCHIVED",
    "MANAGER",
    OTHER_USER_ID,
    "Arquivar",
  ],
];

describe("DocumentDetailPage workflow", () => {
  it.each(workflowCases)(
    "%s updates the document status and shows success",
    async (action, initialStatus, newStatus, role, userId, buttonName) => {
      const initial = documentFixture({ status: initialStatus });
      useDocumentAndAudit(initial);
      let requestCount = 0;
      server.use(
        http.post(
          `*/api/v1/documents/:id/${action}`,
          () => {
            requestCount += 1;
            return HttpResponse.json(
              documentFixture({ status: newStatus }),
            );
          },
        ),
      );

      const confirm =
        action === "archive"
          ? vi.spyOn(window, "confirm").mockReturnValue(true)
          : null;
      const user = userEvent.setup();
      renderDocumentDetail({ role, userId });

      await user.click(
        await screen.findByRole("button", { name: buttonName }),
      );

      expect(
        await screen.findByText(`${buttonName} concluído com sucesso.`),
      ).toBeInTheDocument();
      expect(requestCount).toBe(1);
      expect(
        screen.getByText(
          newStatus === "IN_REVIEW"
            ? "Estado atual: Em revisão."
            : newStatus === "APPROVED"
              ? "Estado atual: Aprovado."
              : newStatus === "ARCHIVED"
                ? "Estado atual: Arquivado."
                : "Estado atual: Rascunho.",
        ),
      ).toBeInTheDocument();

      confirm?.mockRestore();
    },
  );

  it("hides workflow actions while metadata is being edited", async () => {
    useDocumentAndAudit(documentFixture());
    const user = userEvent.setup();
    renderDocumentDetail();

    await user.click(await screen.findByRole("button", { name: "Editar" }));

    expect(
      screen.queryByRole("button", { name: "Enviar para revisão" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText(
        "Conclua ou cancele a edição para executar uma ação de fluxo.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("form", { name: "Editar documento" }),
    ).toBeInTheDocument();
  });

  it("does not show manager-only actions to a collaborator", async () => {
    useDocumentAndAudit(documentFixture({ status: "IN_REVIEW" }));
    renderDocumentDetail();

    await screen.findByRole("heading", { name: "Quality Policy", level: 1 });

    expect(
      screen.queryByRole("button", { name: "Aprovar" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Rejeitar" }),
    ).not.toBeInTheDocument();
  });

  it("cancels archive without calling the backend", async () => {
    useDocumentAndAudit(documentFixture({ status: "APPROVED" }));
    let requests = 0;
    server.use(
      http.post("*/api/v1/documents/:id/archive", () => {
        requests += 1;
        return HttpResponse.json(
          documentFixture({ status: "ARCHIVED" }),
        );
      }),
    );
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);

    const user = userEvent.setup();
    renderDocumentDetail({ role: "MANAGER" });
    await user.click(
      await screen.findByRole("button", { name: "Arquivar" }),
    );

    expect(requests).toBe(0);
    expect(screen.getByText("Estado atual: Aprovado.")).toBeInTheDocument();
    confirm.mockRestore();
  });

  it("refreshes audit after a successful transition", async () => {
    const initial = documentFixture({ status: "IN_REVIEW" });
    let auditRequests = 0;
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json(initial),
      ),
      http.get("*/api/v1/documents/:id/audit", () => {
        auditRequests += 1;
        return HttpResponse.json([]);
      }),
      http.post("*/api/v1/documents/:id/approve", () =>
        HttpResponse.json(documentFixture({ status: "APPROVED" })),
      ),
    );

    const user = userEvent.setup();
    renderDocumentDetail({ role: "MANAGER" });
    await screen.findByText("Nenhuma mudança de estado foi registrada.");
    const beforeTransition = auditRequests;
    await user.click(screen.getByRole("button", { name: "Aprovar" }));

    await waitFor(() => {
      expect(auditRequests).toBeGreaterThan(beforeTransition);
    });
  });

  it.each([
    [403, "Seu perfil não pode executar esta ação."],
    [404, "O documento não foi encontrado ou não está disponível."],
    [409, "O estado do documento mudou. Recarregue a página e tente novamente."],
  ] as const)(
    "shows the safe %s workflow error with correlation ID",
    async (status, expectedMessage) => {
      useDocumentAndAudit(documentFixture());
      server.use(
        http.post("*/api/v1/documents/:id/submit", () =>
          HttpResponse.json(
            {
              status,
              message: "Backend workflow detail",
              correlationId: `workflow-${status}-correlation`,
            },
            { status },
          ),
        ),
      );

      const user = userEvent.setup();
      renderDocumentDetail();
      await user.click(
        await screen.findByRole("button", { name: "Enviar para revisão" }),
      );

      expect(await screen.findByText(expectedMessage)).toBeInTheDocument();
      expect(
        screen.getByText(new RegExp(`workflow-${status}-correlation`)),
      ).toBeInTheDocument();
    },
  );

  it("ends the session when workflow returns 401", async () => {
    useDocumentAndAudit(documentFixture());
    server.use(
      http.post("*/api/v1/documents/:id/submit", () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );

    const user = userEvent.setup();
    renderDocumentDetail();
    await user.click(
      await screen.findByRole("button", { name: "Enviar para revisão" }),
    );

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    expect(sessionStorage.getItem("docfy.accessToken")).toBeNull();
  });
});

describe("DocumentDetailPage Audit Trail", () => {
  it.each(["ADMIN", "MANAGER"] as const)(
    "shows and loads audit for %s",
    async (role) => {
      let requests = 0;
      const event = auditEventFixture();
      server.use(
        http.get("*/api/v1/documents/:id", () =>
          HttpResponse.json(documentFixture()),
        ),
        http.get("*/api/v1/documents/:id/audit", () => {
          requests += 1;
          return HttpResponse.json([event]);
        }),
      );

      renderDocumentDetail({ role });

      expect(
        await screen.findByRole("heading", { name: "Audit Trail" }),
      ).toBeInTheDocument();
      expect(await screen.findByText("Enviado para revisão")).toBeInTheDocument();
      expect(requests).toBe(1);
    },
  );

  it("does not render or request audit for collaborators", async () => {
    let auditRequests = 0;
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json(documentFixture()),
      ),
      http.get("*/api/v1/documents/:id/audit", () => {
        auditRequests += 1;
        return HttpResponse.json([]);
      }),
    );

    renderDocumentDetail();
    await screen.findByRole("heading", { name: "Quality Policy", level: 1 });

    expect(
      screen.queryByRole("heading", { name: "Audit Trail" }),
    ).not.toBeInTheDocument();
    expect(auditRequests).toBe(0);
  });

  it("shows audit loading and then the empty state", async () => {
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json(documentFixture()),
      ),
      http.get("*/api/v1/documents/:id/audit", async () => {
        await delay(40);
        return HttpResponse.json([]);
      }),
    );

    renderDocumentDetail({ role: "MANAGER" });

    expect(
      await screen.findByText("Carregando histórico..."),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("Nenhuma mudança de estado foi registrada."),
    ).toBeInTheDocument();
  });

  it("preserves event order and presents all audit fields", async () => {
    const approved = auditEventFixture({
      id: "dddddddd-dddd-dddd-dddd-dddddddddddd",
      action: "DOCUMENT_APPROVED",
      previousStatus: "IN_REVIEW",
      newStatus: "APPROVED",
      occurredAt: "2026-08-30T15:00:00Z",
      correlationId: null,
    });
    const submitted = auditEventFixture();
    useDocumentAndAudit(documentFixture(), [approved, submitted]);

    renderDocumentDetail({ role: "ADMIN" });

    const items = await screen.findAllByRole("listitem");
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent("Documento aprovado");
    expect(items[0]).toHaveTextContent("Em revisão → Aprovado");
    expect(items[0]).toHaveTextContent(formatDateTime(approved.occurredAt));
    expect(items[0]).toHaveTextContent(`Ator ${approved.actorId}`);
    expect(items[1]).toHaveTextContent("Enviado para revisão");
    expect(items[1]).toHaveTextContent("Rascunho → Em revisão");
    expect(items[1]).toHaveTextContent("Correlação: audit-correlation");
  });

  it("shows an audit error with correlation ID and retries", async () => {
    let requests = 0;
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json(documentFixture()),
      ),
      http.get("*/api/v1/documents/:id/audit", () => {
        requests += 1;
        return requests === 1
          ? HttpResponse.json(
              {
                status: 500,
                message: "Audit unavailable",
                correlationId: "audit-error-correlation",
              },
              { status: 500 },
            )
          : HttpResponse.json([auditEventFixture()]);
      }),
    );

    const user = userEvent.setup();
    renderDocumentDetail({ role: "MANAGER" });

    expect(
      await screen.findByText(
        "Não foi possível carregar o histórico do documento.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText(/audit-error-correlation/)).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );

    expect(await screen.findByText("Enviado para revisão")).toBeInTheDocument();
    expect(requests).toBe(2);
  });

  it("ends the session when audit returns 401", async () => {
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json(documentFixture()),
      ),
      http.get("*/api/v1/documents/:id/audit", () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );

    renderDocumentDetail({ role: "MANAGER" });

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    expect(sessionStorage.getItem("docfy.accessToken")).toBeNull();
  });
});
