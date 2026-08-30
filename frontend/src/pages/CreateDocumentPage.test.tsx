import {
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
  useParams,
} from "react-router-dom";
import { describe, expect, it } from "vitest";

import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { CreateDocumentPage } from "@/pages/CreateDocumentPage";
import { server } from "@/test/server";
import { createTestToken } from "@/test/token";
import type { Document } from "@/types/document";

const STORAGE_KEY = "docfy.accessToken";

const createdDocument: Document = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  title: "Quality Policy",
  description: "Policy description",
  status: "DRAFT",
  createdBy: "11111111-1111-1111-1111-111111111111",
  createdAt: "2026-08-30T12:00:00Z",
  updatedAt: "2026-08-30T12:00:00Z",
};

function CreatedDocumentTarget() {
  const { id } = useParams();
  const location = useLocation();
  const state = location.state as { notice?: string } | null;

  return (
    <div>
      <p>Documento criado: {id}</p>
      <p>{state?.notice}</p>
    </div>
  );
}

function renderCreatePage() {
  sessionStorage.setItem(STORAGE_KEY, createTestToken());

  return render(
    <MemoryRouter initialEntries={["/documents/new"]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<p>Acesse o Docfy</p>} />
          <Route element={<ProtectedRoute />}>
            <Route
              path="/documents/new"
              element={<CreateDocumentPage />}
            />
            <Route
              path="/documents/:id"
              element={<CreatedDocumentTarget />}
            />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("CreateDocumentPage", () => {
  it("renders the creation form with accessible fields", () => {
    renderCreatePage();

    expect(
      screen.getByRole("heading", { name: "Novo documento" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Título")).toBeRequired();
    expect(screen.getByLabelText(/Descrição/)).toBeInTheDocument();
  });

  it("rejects a blank title without sending a request", async () => {
    let requests = 0;
    server.use(
      http.post("*/api/v1/documents", () => {
        requests += 1;
        return HttpResponse.json(createdDocument);
      }),
    );

    const user = userEvent.setup();
    renderCreatePage();
    await user.type(screen.getByLabelText("Título"), "   ");
    await user.click(
      screen.getByRole("button", { name: "Criar rascunho" }),
    );

    expect(
      await screen.findByText("Informe um título para o documento."),
    ).toBeInTheDocument();
    expect(requests).toBe(0);
  });

  it("rejects titles longer than 255 characters", async () => {
    renderCreatePage();
    fireEvent.change(screen.getByLabelText("Título"), {
      target: { value: "A".repeat(256) },
    });
    fireEvent.submit(
      screen.getByRole("form", { name: "Criar documento" }),
    );

    expect(
      await screen.findByText(
        "O título deve ter no máximo 255 caracteres.",
      ),
    ).toBeInTheDocument();
  });

  it("posts the normalized document and redirects with success notice", async () => {
    server.use(
      http.post("*/api/v1/documents", async ({ request }) => {
        expect(request.headers.get("authorization")).toMatch(/^Bearer /);
        expect(await request.json()).toEqual({
          title: "Quality Policy",
          description: "Policy description",
        });
        return HttpResponse.json(createdDocument, { status: 201 });
      }),
    );

    const user = userEvent.setup();
    renderCreatePage();
    await user.type(
      screen.getByLabelText("Título"),
      "  Quality Policy  ",
    );
    await user.type(
      screen.getByLabelText(/Descrição/),
      "  Policy description  ",
    );
    await user.click(
      screen.getByRole("button", { name: "Criar rascunho" }),
    );

    expect(
      await screen.findByText(
        `Documento criado: ${createdDocument.id}`,
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Documento criado com sucesso."),
    ).toBeInTheDocument();
  });

  it("shows a structured validation error with correlation ID", async () => {
    server.use(
      http.post("*/api/v1/documents", () =>
        HttpResponse.json(
          {
            status: 400,
            message: "Invalid payload",
            correlationId: "create-400-correlation",
          },
          { status: 400 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderCreatePage();
    await user.type(screen.getByLabelText("Título"), "Policy");
    await user.click(
      screen.getByRole("button", { name: "Criar rascunho" }),
    );

    expect(
      await screen.findByText("Revise os dados informados."),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/create-400-correlation/),
    ).toBeInTheDocument();
  });

  it("ends the session when creation returns 401", async () => {
    server.use(
      http.post("*/api/v1/documents", () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );

    const user = userEvent.setup();
    renderCreatePage();
    await user.type(screen.getByLabelText("Título"), "Policy");
    await user.click(
      screen.getByRole("button", { name: "Criar rascunho" }),
    );

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("shows an unexpected error and preserves its correlation ID", async () => {
    server.use(
      http.post("*/api/v1/documents", () =>
        HttpResponse.json(
          {
            status: 500,
            message: "Internal error",
            correlationId: "create-500-correlation",
          },
          { status: 500 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderCreatePage();
    await user.type(screen.getByLabelText("Título"), "Policy");
    await user.click(
      screen.getByRole("button", { name: "Criar rascunho" }),
    );

    expect(
      await screen.findByText("Não foi possível criar o documento."),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/create-500-correlation/),
    ).toBeInTheDocument();
  });
});
