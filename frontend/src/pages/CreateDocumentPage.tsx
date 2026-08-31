import { ArrowLeft, FilePlus2 } from "lucide-react";
import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
} from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/client";
import { createDocument, listCategories } from "@/api/documents";
import { useAuth } from "@/auth/useAuth";
import { AppShell } from "@/components/app-shell";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { Category } from "@/types/document";

interface FormError {
  message: string;
  correlationId?: string;
}

export function CreateDocumentPage() {
  const { accessToken, logout } = useAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [categoriesError, setCategoriesError] =
    useState<FormError | null>(null);
  const [categoriesReloadKey, setCategoriesReloadKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<FormError | null>(null);

  const retryCategories = useCallback(() => {
    setCategoriesReloadKey((current) => current + 1);
  }, []);

  useEffect(() => {
    const token = accessToken;

    if (!token) {
      return;
    }

    const controller = new AbortController();

    async function loadAvailableCategories(authToken: string) {
      setCategoriesError(null);

      try {
        setCategories(
          await listCategories({
            accessToken: authToken,
            signal: controller.signal,
          }),
        );
      } catch (caughtError) {
        if (controller.signal.aborted) {
          return;
        }

        if (caughtError instanceof ApiError && caughtError.status === 401) {
          logout();
          return;
        }

        setCategoriesError({
          message: "Não foi possível carregar as categorias.",
          correlationId:
            caughtError instanceof ApiError
              ? caughtError.correlationId
              : undefined,
        });
      }
    }

    void loadAvailableCategories(token);

    return () => controller.abort();
  }, [accessToken, categoriesReloadKey, logout]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedTitle = title.trim();

    if (!normalizedTitle) {
      setError({ message: "Informe um título para o documento." });
      return;
    }

    if (normalizedTitle.length > 255) {
      setError({
        message: "O título deve ter no máximo 255 caracteres.",
      });
      return;
    }

    if (!categoryId) {
      setError({ message: "Selecione uma categoria." });
      return;
    }

    if (!accessToken) {
      logout();
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      const document = await createDocument({
        accessToken,
        document: {
          title: normalizedTitle,
          description: description.trim() || null,
          categoryId,
        },
      });

      navigate(`/documents/${document.id}`, {
        replace: true,
        state: { notice: "Documento criado com sucesso." },
      });
    } catch (caughtError) {
      if (
        caughtError instanceof ApiError &&
        caughtError.status === 401
      ) {
        logout();
        return;
      }

      setError({
        message:
          caughtError instanceof ApiError && caughtError.status === 400
            ? "Revise os dados informados."
            : "Não foi possível criar o documento.",
        correlationId:
          caughtError instanceof ApiError
            ? caughtError.correlationId
            : undefined,
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-3xl space-y-8">
        <PageHeader
          title="Novo documento"
          description="Crie um rascunho com as informações iniciais do documento."
          action={
            <Button variant="outline" asChild>
              <Link to="/documents">
                <ArrowLeft />
                Voltar
              </Link>
            </Button>
          }
        />

        <form
          aria-label="Criar documento"
          onSubmit={handleSubmit}
          className="space-y-6 rounded-xl border border-border bg-card p-6 shadow-sm"
        >
          <div className="flex items-center gap-3 border-b border-border pb-5">
            <div className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
              <FilePlus2 className="size-5" />
            </div>
            <div>
              <h2 className="font-semibold text-foreground">
                Informações do documento
              </h2>
              <p className="text-sm text-muted-foreground">
                O documento será criado automaticamente como rascunho.
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="document-title">Título</Label>
            <Input
              id="document-title"
              name="title"
              required
              maxLength={255}
              autoFocus
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              aria-invalid={Boolean(error && !title.trim())}
            />
            <p className="text-xs text-muted-foreground">
              {title.length}/255 caracteres
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="document-category">Categoria</Label>
            <select
              id="document-category"
              name="categoryId"
              required
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
              disabled={!categories || Boolean(categoriesError)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">
                {categories ? "Selecione" : "Carregando categorias..."}
              </option>
              {categories?.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          {categoriesError ? (
            <div
              role="alert"
              className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"
            >
              <p className="text-sm text-destructive">
                {categoriesError.message}
              </p>
              {categoriesError.correlationId ? (
                <p className="mt-1 text-xs text-muted-foreground">
                  Código de rastreio: {categoriesError.correlationId}
                </p>
              ) : null}
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-3"
                onClick={retryCategories}
              >
                Tentar novamente
              </Button>
            </div>
          ) : null}

          <div className="space-y-2">
            <Label htmlFor="document-description">
              Descrição <span className="text-muted-foreground">(opcional)</span>
            </Label>
            <Textarea
              id="document-description"
              name="description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Contexto, objetivo ou observações sobre o documento."
            />
          </div>

          {error ? (
            <div
              role="alert"
              className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"
            >
              <p className="text-sm text-destructive">{error.message}</p>
              {error.correlationId ? (
                <p className="mt-1 text-xs text-muted-foreground">
                  Código de rastreio: {error.correlationId}
                </p>
              ) : null}
            </div>
          ) : null}

          <div className="flex flex-col-reverse gap-3 border-t border-border pt-5 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              asChild
              disabled={isSubmitting}
            >
              <Link to="/documents">Cancelar</Link>
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Criando..." : "Criar rascunho"}
            </Button>
          </div>
        </form>
      </div>
    </AppShell>
  );
}
