import { HttpResponse, http } from "msw";
import { useState } from "react";
import {
  MemoryRouter,
  Route,
  Routes,
} from "react-router-dom";
import {
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { useAuth } from "@/auth/useAuth";
import { LoginPage } from "@/pages/LoginPage";
import { server } from "@/test/server";
import { createTestToken } from "@/test/token";

const STORAGE_KEY = "docfy.accessToken";

function PrivatePage() {
  const { logout } = useAuth();
  const [visited] = useState(true);

  return (
    <div>
      <p>{visited ? "Área protegida" : ""}</p>
      <button type="button" onClick={logout}>
        Encerrar sessão
      </button>
    </div>
  );
}

function renderAuthFlow(initialRoute = "/login") {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/documents" element={<PrivatePage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("authentication flow", () => {
  it("authenticates with the real login contract", async () => {
    const token = createTestToken();

    server.use(
      http.post("*/api/v1/auth/login", async ({ request }) => {
        expect(await request.json()).toEqual({
          email: "ana@docfy.local",
          password: "local-password",
        });

        return HttpResponse.json({
          accessToken: token,
          tokenType: "Bearer",
          expiresIn: 3600,
        });
      }),
    );

    const user = userEvent.setup();
    renderAuthFlow();

    await user.type(
      screen.getByLabelText("E-mail"),
      "ana@docfy.local",
    );
    await user.type(
      screen.getByLabelText("Senha"),
      "local-password",
    );
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Área protegida")).toBeInTheDocument();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(token);
  });

  it("shows the structured error for invalid credentials", async () => {
    server.use(
      http.post("*/api/v1/auth/login", () =>
        HttpResponse.json(
          {
            status: 401,
            message: "Invalid credentials",
            correlationId: "login-correlation-id",
          },
          { status: 401 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderAuthFlow();

    await user.type(screen.getByLabelText("E-mail"), "ana@docfy.local");
    await user.type(screen.getByLabelText("Senha"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    expect(
      await screen.findByText("E-mail ou senha inválidos."),
    ).toBeInTheDocument();
    expect(screen.getByText(/login-correlation-id/)).toBeInTheDocument();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("redirects an unauthenticated visitor away from a protected route", async () => {
    renderAuthFlow("/documents");

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    expect(screen.queryByText("Área protegida")).not.toBeInTheDocument();
  });

  it("ends the authenticated session on logout", async () => {
    sessionStorage.setItem(STORAGE_KEY, createTestToken());
    const user = userEvent.setup();

    renderAuthFlow("/documents");

    await user.click(
      await screen.findByRole("button", { name: "Encerrar sessão" }),
    );

    expect(await screen.findByText("Acesse o Docfy")).toBeInTheDocument();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("rejects an expired token restored from session storage", async () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      createTestToken({ expiresAt: Date.now() - 1_000 }),
    );

    renderAuthFlow("/documents");

    await waitFor(() => {
      expect(screen.getByText("Acesse o Docfy")).toBeInTheDocument();
    });
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });
});
