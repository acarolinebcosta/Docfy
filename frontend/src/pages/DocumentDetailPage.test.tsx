import {
  fireEvent,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, delay, http } from "msw";
import { describe, expect, it } from "vitest";

import { formatDateTime } from "@/lib/date";
import { server } from "@/test/server";
import {
  DOCUMENT_A_ID,
  OTHER_USER_ID,
  OWNER_ID,
  documentFixture,
  renderDocumentDetail,
} from "@/test/document-detail";
import type { Role } from "@/types/auth";
import type { Document } from "@/types/document";

function useDocumentResponse(
  document: Document,
  auditResponse: unknown[] = [],
) {
  server.use(
    http.get("*/api/v1/documents/:id", () =>
      HttpResponse.json(document),
    ),
    http.get("*/api/v1/documents/:id/audit", () =>
      HttpResponse.json(auditResponse),
    ),
  );
}

describe("DocumentDetailPage loading and detail", () => {
  it("shows loading then the real document metadata", async () => {
    const document = documentFixture();
    server.use(
      http.get("*/api/v1/documents/:id", async () => {
        await delay(40);
        return HttpResponse.json(document);
      }),
    );

    renderDocumentDetail();

    expect(screen.getByText("Carregando documento")).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: document.title, level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByText(document.description!)).toBeInTheDocument();
    expect(screen.getByText("Você")).toBeInTheDocument();
    expect(
      screen.getByText(formatDateTime(document.createdAt)),
    ).toBeInTheDocument();
    expect(
      screen.getByText(formatDateTime(document.updatedAt)),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Rascunho").length).toBeGreaterThan(0);
    expect(screen.queryByText("passwordHash")).not.toBeInTheDocument();
  });

  it("shows the empty description and another owner safely", async () => {
    useDocumentResponse(
      documentFixture({
        description: null,
        createdBy: OTHER_USER_ID,
        status: "APPROVED",
      }),
    );

    renderDocumentDetail();

    expect(
      await screen.findByText("Este documento não possui descrição."),
    ).toBeInTheDocument();
    expect(screen.getByText("Outro usuário")).toBeInTheDocument();
    expect(screen.getAllByText("Aprovado").length).toBeGreaterThan(0);
  });

  it("retries a failed detail request", async () => {
    let requests = 0;
    server.use(
      http.get("*/api/v1/documents/:id", () => {
        requests += 1;
        return requests === 1
          ? HttpResponse.json(
              {
                status: 500,
                message: "Internal error",
                correlationId: "detail-retry-correlation",
              },
              { status: 500 },
            )
          : HttpResponse.json(documentFixture());
      }),
    );

    const user = userEvent.setup();
    renderDocumentDetail();

    expect(
      await screen.findByText("Não foi possível carregar o documento."),
    ).toBeInTheDocument();
    expect(screen.getByText(/detail-retry-correlation/)).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );

    expect(
      await screen.findByRole("heading", { name: "Quality Policy", level: 1 }),
    ).toBeInTheDocument();
    expect(requests).toBe(2);
  });

  it("ends the session when detail loading returns 401", async () => {
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );

    renderDocumentDetail();

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    expect(sessionStorage.getItem("docfy.accessToken")).toBeNull();
  });

  it("uses a safe 404 message without disclosing resource existence", async () => {
    server.use(
      http.get("*/api/v1/documents/:id", () =>
        HttpResponse.json(
          { status: 404, message: "Document not found" },
          { status: 404 },
        ),
      ),
    );

    renderDocumentDetail();

    expect(
      await screen.findByText(
        "O documento não foi encontrado ou não está disponível para o seu perfil.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText(/existe mas/i)).not.toBeInTheDocument();
  });

  it("does not keep document A visible when route B returns 404", async () => {
    server.use(
      http.get("*/api/v1/documents/:id", ({ params }) =>
        params.id === DOCUMENT_A_ID
          ? HttpResponse.json(documentFixture({ title: "Documento A" }))
          : HttpResponse.json(
              { status: 404, message: "Document not found" },
              { status: 404 },
            ),
      ),
    );

    const user = userEvent.setup();
    renderDocumentDetail();
    expect(
      await screen.findByRole("heading", { name: "Documento A", level: 1 }),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("link", { name: "Abrir documento B" }),
    );

    expect(
      await screen.findByText(
        "O documento não foi encontrado ou não está disponível para o seu perfil.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText("Documento A")).not.toBeInTheDocument();
    expect(screen.queryByText("Policy description")).not.toBeInTheDocument();
  });
});

describe("DocumentDetailPage edit UX", () => {
  it.each([
    ["COLLABORATOR", OWNER_ID, "DRAFT", true],
    ["COLLABORATOR", OTHER_USER_ID, "DRAFT", false],
    ["MANAGER", OTHER_USER_ID, "DRAFT", true],
    ["ADMIN", OTHER_USER_ID, "DRAFT", true],
    ["ADMIN", OWNER_ID, "APPROVED", false],
  ] as const)(
    "shows edit=%s for role %s user/status context %#",
    async (role, userId, status, expected) => {
      useDocumentResponse(documentFixture({ status }));
      renderDocumentDetail({ role: role as Role, userId });

      await screen.findByRole("heading", { name: "Quality Policy", level: 1 });

      if (expected) {
        expect(
          screen.getByRole("button", { name: "Editar" }),
        ).toBeInTheDocument();
      } else {
        expect(
          screen.queryByRole("button", { name: "Editar" }),
        ).not.toBeInTheDocument();
      }
    },
  );

  it("validates blank and oversized titles", async () => {
    useDocumentResponse(documentFixture());
    const user = userEvent.setup();
    renderDocumentDetail();
    await user.click(await screen.findByRole("button", { name: "Editar" }));

    const title = screen.getByLabelText("Título");
    await user.clear(title);
    await user.type(title, "   ");
    await user.click(
      screen.getByRole("button", { name: "Salvar alterações" }),
    );
    expect(
      await screen.findByText("Informe um título para o documento."),
    ).toBeInTheDocument();

    fireEvent.change(title, { target: { value: "A".repeat(256) } });
    fireEvent.submit(
      screen.getByRole("form", { name: "Editar documento" }),
    );
    expect(
      await screen.findByText(
        "O título deve ter no máximo 255 caracteres.",
      ),
    ).toBeInTheDocument();
  });

  it("does not send PATCH when no field changed", async () => {
    let patchRequests = 0;
    useDocumentResponse(documentFixture());
    server.use(
      http.patch("*/api/v1/documents/:id", () => {
        patchRequests += 1;
        return HttpResponse.json(documentFixture());
      }),
    );

    const user = userEvent.setup();
    renderDocumentDetail();
    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.click(
      screen.getByRole("button", { name: "Salvar alterações" }),
    );

    expect(
      await screen.findByText("Nenhuma alteração foi informada."),
    ).toBeInTheDocument();
    expect(patchRequests).toBe(0);
  });

  it("patches only a changed title and shows success", async () => {
    useDocumentResponse(documentFixture());
    server.use(
      http.patch("*/api/v1/documents/:id", async ({ request }) => {
        expect(await request.json()).toEqual({ title: "Updated Policy" });
        return HttpResponse.json(
          documentFixture({ title: "Updated Policy" }),
        );
      }),
    );

    const user = userEvent.setup();
    renderDocumentDetail();
    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.clear(screen.getByLabelText("Título"));
    await user.type(screen.getByLabelText("Título"), "Updated Policy");
    await user.click(
      screen.getByRole("button", { name: "Salvar alterações" }),
    );

    expect(
      await screen.findByText("Documento atualizado com sucesso."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Updated Policy", level: 1 }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("form", { name: "Editar documento" }),
    ).not.toBeInTheDocument();
  });

  it("clears description with a null-only PATCH", async () => {
    useDocumentResponse(documentFixture());
    server.use(
      http.patch("*/api/v1/documents/:id", async ({ request }) => {
        expect(await request.json()).toEqual({ description: null });
        return HttpResponse.json(documentFixture({ description: null }));
      }),
    );

    const user = userEvent.setup();
    renderDocumentDetail();
    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.clear(screen.getByLabelText("Descrição"));
    await user.click(
      screen.getByRole("button", { name: "Salvar alterações" }),
    );

    expect(
      await screen.findByText("Este documento não possui descrição."),
    ).toBeInTheDocument();
  });

  it.each([
    [400, "Revise os dados informados."],
    [403, "Este documento não pode mais ser editado."],
  ] as const)(
    "shows a structured %s edit error",
    async (status, expectedMessage) => {
      useDocumentResponse(documentFixture());
      server.use(
        http.patch("*/api/v1/documents/:id", () =>
          HttpResponse.json(
            {
              status,
              message: "Backend message",
              correlationId: `edit-${status}-correlation`,
            },
            { status },
          ),
        ),
      );

      const user = userEvent.setup();
      renderDocumentDetail();
      await user.click(await screen.findByRole("button", { name: "Editar" }));
      await user.clear(screen.getByLabelText("Título"));
      await user.type(screen.getByLabelText("Título"), "Updated Policy");
      await user.click(
        screen.getByRole("button", { name: "Salvar alterações" }),
      );

      expect(await screen.findByText(expectedMessage)).toBeInTheDocument();
      expect(
        screen.getByText(new RegExp(`edit-${status}-correlation`)),
      ).toBeInTheDocument();
    },
  );

  it("ends the session when PATCH returns 401", async () => {
    useDocumentResponse(documentFixture());
    server.use(
      http.patch("*/api/v1/documents/:id", () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );

    const user = userEvent.setup();
    renderDocumentDetail();
    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.clear(screen.getByLabelText("Título"));
    await user.type(screen.getByLabelText("Título"), "Updated Policy");
    await user.click(
      screen.getByRole("button", { name: "Salvar alterações" }),
    );

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    await waitFor(() => {
      expect(sessionStorage.getItem("docfy.accessToken")).toBeNull();
    });
  });
});
