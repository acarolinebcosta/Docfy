import {
  useState,
  type FormEvent,
} from "react";
import {
  Navigate,
  useLocation,
  useNavigate,
} from "react-router-dom";

import { ApiError } from "@/api/client";
import { useAuth } from "@/auth/useAuth";
import { DocfyLogo } from "@/components/logo";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface LoginError {
  message: string;
  correlationId?: string;
}

export function LoginPage() {
  const {
    login,
    isAuthenticated,
  } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const [isSubmitting, setIsSubmitting] =
    useState(false);

  const [error, setError] =
    useState<LoginError | null>(null);

  if (isAuthenticated) {
    return (
      <Navigate
        to="/documents"
        replace
      />
    );
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    setError(null);
    setIsSubmitting(true);

    try {
      await login({
        email,
        password,
      });

      const state = location.state as {
        from?: string;
      } | null;

      navigate(
        state?.from ?? "/documents",
        {
          replace: true,
        },
      );
    } catch (caughtError) {
      if (caughtError instanceof ApiError) {
        let message =
          "Não foi possível entrar. Tente novamente.";

        if (caughtError.status === 400) {
          message =
            "Revise o e-mail e a senha informados.";
        }

        if (caughtError.status === 401) {
          message =
            "E-mail ou senha inválidos.";
        }

        setError({
          message,
          correlationId:
            caughtError.correlationId,
        });
      } else {
        setError({
          message:
            "Não foi possível entrar. Tente novamente.",
        });
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12">
      <div className="w-full max-w-md">
        <div className="mb-10 flex justify-center">
          <DocfyLogo />
        </div>

        <div className="rounded-xl border border-border bg-card p-8 shadow-sm">
          <div className="mb-8 space-y-2 text-center">
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">
              Acesse o Docfy
            </h1>

            <p className="text-sm text-muted-foreground">
              Entre com suas credenciais para
              acessar seus documentos.
            </p>
          </div>

          <form
            className="space-y-5"
            onSubmit={handleSubmit}
          >
            <div className="space-y-2">
              <Label htmlFor="email">
                E-mail
              </Label>

              <Input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) =>
                  setEmail(
                    event.target.value,
                  )
                }
                placeholder="voce@empresa.com"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">
                Senha
              </Label>

              <Input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(event) =>
                  setPassword(
                    event.target.value,
                  )
                }
              />
            </div>

            {error && (
              <div
                role="alert"
                aria-live="polite"
                className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"
              >
                <p className="text-sm text-destructive">
                  {error.message}
                </p>

                {error.correlationId && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Código de rastreio:{" "}
                    {error.correlationId}
                  </p>
                )}
              </div>
            )}

            <Button
              type="submit"
              className="w-full"
              disabled={isSubmitting}
            >
              {isSubmitting
                ? "Entrando..."
                : "Entrar"}
            </Button>
          </form>
        </div>
      </div>
    </main>
  );
}