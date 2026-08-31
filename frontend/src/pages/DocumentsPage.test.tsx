import { delay, HttpResponse, http } from "msw";
import {
  MemoryRouter,
  Route,
  Routes,
} from "react-router-dom";
import {
  render,
  screen,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { DocumentsPage } from "@/pages/DocumentsPage";
import { server } from "@/test/server";
import { createTestToken } from "@/test/token";
import type {
  Document,
  DocumentPage,
} from "@/types/document";

const STORAGE_KEY = "docfy.accessToken";
const CONTRACT_CATEGORY_ID = "11111111-0000-0000-0000-000000000003";

function documentItem(
  overrides: Partial<Document> = {},
): Document {
  return {
    id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    documentCode: "DOC-000001",
    title: "Quality Policy",
    description: "Quality principles",
    status: "DRAFT",
    category: {
      id: "11111111-0000-0000-0000-000000000005",
      name: "Regulation",
    },
    createdBy: "11111111-1111-1111-1111-111111111111",
    createdAt: "2026-08-29T12:00:00Z",
    updatedAt: "2026-08-30T12:00:00Z",
    ...overrides,
  };
}

function documentPage(
  items: Document[],
  overrides: Partial<DocumentPage> = {},
): DocumentPage {
  return {
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: items.length > 0 ? 1 : 0,
    ...overrides,
  };
}

function renderDocuments() {
  server.use(
    http.get("*/api/v1/categories", () =>
      HttpResponse.json([
        { id: CONTRACT_CATEGORY_ID, name: "Contract" },
      ]),
    ),
  );
  sessionStorage.setItem(STORAGE_KEY, createTestToken());

  return render(
    <MemoryRouter initialEntries={["/documents"]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<p>Login necessário</p>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/documents" element={<DocumentsPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("DocumentsPage", () => {
  it("shows loading and then renders documents from the backend", async () => {
    server.use(
      http.get("*/api/v1/documents", async () => {
        await delay(80);
        return HttpResponse.json(documentPage([documentItem()]));
      }),
    );

    renderDocuments();

    expect(screen.getByText("Carregando documentos")).toBeInTheDocument();
    expect(await screen.findByText("Quality Policy")).toBeInTheDocument();
    expect(screen.getByText("DOC-000001")).toBeInTheDocument();
    expect(screen.getByText("Regulation")).toBeInTheDocument();
    expect(
      screen.getByText("Rascunho", { selector: "span" }),
    ).toBeInTheDocument();
    expect(screen.getByText("1 documento")).toBeInTheDocument();
  });

  it("renders the empty state returned by the backend", async () => {
    server.use(
      http.get("*/api/v1/documents", () =>
        HttpResponse.json(documentPage([])),
      ),
    );

    renderDocuments();

    expect(
      await screen.findByText("Nenhum documento encontrado"),
    ).toBeInTheDocument();
  });

  it("shows a correlation id and retries a failed request", async () => {
    let attempts = 0;

    server.use(
      http.get("*/api/v1/documents", () => {
        attempts += 1;

        if (attempts === 1) {
          return HttpResponse.json(
            {
              status: 500,
              message: "Unexpected error",
              correlationId: "documents-correlation-id",
            },
            { status: 500 },
          );
        }

        return HttpResponse.json(
          documentPage([
            documentItem({ title: "Recovered document" }),
          ]),
        );
      }),
    );

    const user = userEvent.setup();
    renderDocuments();

    expect(
      await screen.findByText("documents-correlation-id", { exact: false }),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );

    expect(
      await screen.findByText("Recovered document"),
    ).toBeInTheDocument();
    expect(attempts).toBe(2);
  });

  it("ends the session when the backend returns 401", async () => {
    server.use(
      http.get("*/api/v1/documents", () =>
        HttpResponse.json(
          {
            status: 401,
            message: "Authentication required",
          },
          { status: 401 },
        ),
      ),
    );

    renderDocuments();

    expect(await screen.findByText("Login necessário")).toBeInTheDocument();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("requests the next page from the backend", async () => {
    const requestedPages: string[] = [];

    server.use(
      http.get("*/api/v1/documents", ({ request }) => {
        const requestedPage = new URL(request.url).searchParams.get("page");
        requestedPages.push(requestedPage ?? "");

        if (requestedPage === "1") {
          return HttpResponse.json(
            documentPage(
              [
                documentItem({
                  id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                  title: "Second page document",
                }),
              ],
              {
                page: 1,
                totalElements: 21,
                totalPages: 2,
              },
            ),
          );
        }

        return HttpResponse.json(
          documentPage([documentItem()], {
            totalElements: 21,
            totalPages: 2,
          }),
        );
      }),
    );

    const user = userEvent.setup();
    renderDocuments();

    expect(await screen.findByText("Quality Policy")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Próxima/ }));

    expect(
      await screen.findByText("Second page document"),
    ).toBeInTheDocument();
    expect(requestedPages).toEqual(["0", "1"]);
  });

  it("sends combined filters and resets pagination", async () => {
    const requests: URLSearchParams[] = [];

    server.use(
      http.get("*/api/v1/documents", ({ request }) => {
        const parameters = new URL(request.url).searchParams;
        requests.push(new URLSearchParams(parameters));

        return HttpResponse.json(
          documentPage([documentItem()], {
            page: Number(parameters.get("page") ?? 0),
            totalElements: 21,
            totalPages: 2,
          }),
        );
      }),
    );

    const user = userEvent.setup();
    renderDocuments();

    expect(await screen.findByText("Quality Policy")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Próxima/ }));

    await screen.findByText("Página 2 de 2");
    await user.type(
      screen.getByLabelText("Título ou código"),
      "  DOC-000001  ",
    );
    await user.selectOptions(
      await screen.findByLabelText("Categoria"),
      CONTRACT_CATEGORY_ID,
    );
    await user.selectOptions(screen.getByLabelText("Status"), "APPROVED");
    await user.click(screen.getByRole("button", { name: "Buscar" }));

    await screen.findByText("Página 1 de 2");

    const combinedRequest = requests.find(
      (parameters) =>
        parameters.get("search") === "DOC-000001" &&
        parameters.get("categoryId") === CONTRACT_CATEGORY_ID &&
        parameters.get("status") === "APPROVED",
    );

    expect(combinedRequest?.get("page")).toBe("0");
    expect(combinedRequest?.get("size")).toBe("20");
  });

  it("clears active filters and requests the unfiltered first page", async () => {
    const requests: URLSearchParams[] = [];

    server.use(
      http.get("*/api/v1/documents", ({ request }) => {
        requests.push(new URL(request.url).searchParams);
        return HttpResponse.json(documentPage([]));
      }),
    );

    const user = userEvent.setup();
    renderDocuments();

    await screen.findByText("Nenhum documento encontrado");
    await user.type(screen.getByLabelText("Título ou código"), "Policy");
    await user.click(screen.getByRole("button", { name: "Buscar" }));

    expect(
      await screen.findByText(
        "Nenhum documento visível corresponde aos critérios informados.",
      ),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Limpar filtros" }));

    const lastRequest = requests.at(-1);
    expect(lastRequest?.get("page")).toBe("0");
    expect(lastRequest?.has("search")).toBe(false);
    expect(lastRequest?.has("categoryId")).toBe(false);
    expect(lastRequest?.has("status")).toBe(false);
  });
});
