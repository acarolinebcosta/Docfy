import {
  ChevronLeft,
  ChevronRight,
  FileText,
  Plus,
  Search,
  SlidersHorizontal,
  X,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
} from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/client";
import { listCategories, listDocuments } from "@/api/documents";
import { useAuth } from "@/auth/useAuth";
import { ApiErrorState } from "@/components/api-error-state";
import { AppShell } from "@/components/app-shell";
import { DocumentStatusBadge } from "@/components/document-status-badge";
import { EmptyState } from "@/components/empty-state";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatDateTime } from "@/lib/date";
import { DOCUMENT_STATUS_LABELS } from "@/lib/document-status";
import type {
  Category,
  DocumentPage,
  DocumentStatus,
} from "@/types/document";

const PAGE_SIZE = 20;

interface PageError {
  message: string;
  correlationId?: string;
}

export function DocumentsPage() {
  const {
    accessToken,
    logout,
  } = useAuth();

  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [status, setStatus] = useState<DocumentStatus | "">("");
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [categoriesError, setCategoriesError] =
    useState<PageError | null>(null);
  const [categoriesReloadKey, setCategoriesReloadKey] = useState(0);
  const [documents, setDocuments] =
    useState<DocumentPage | null>(null);
  const [isLoading, setIsLoading] =
    useState(true);
  const [error, setError] =
    useState<PageError | null>(null);
  const [reloadKey, setReloadKey] =
    useState(0);

  const retry = useCallback(() => {
    setReloadKey((current) => current + 1);
  }, []);

  const hasActiveFilters = Boolean(search || categoryId || status);

  function applySearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  }

  function clearFilters() {
    setSearchInput("");
    setSearch("");
    setCategoryId("");
    setStatus("");
    setPage(0);
  }

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

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    const controller = new AbortController();

    async function loadDocuments() {
      setIsLoading(true);
      setError(null);

      try {
        const response = await listDocuments({
          accessToken: token,
          page,
          size: PAGE_SIZE,
          search,
          categoryId,
          status: status || undefined,
          signal: controller.signal,
        });

        setDocuments(response);
      } catch (caughtError) {
        if (controller.signal.aborted) {
          return;
        }

        if (
          caughtError instanceof ApiError &&
          caughtError.status === 401
        ) {
          logout();
          return;
        }

        setError({
          message:
            caughtError instanceof ApiError
              ? "Não foi possível carregar os documentos."
              : "Não foi possível conectar ao servidor.",
          correlationId:
            caughtError instanceof ApiError
              ? caughtError.correlationId
              : undefined,
        });
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadDocuments();

    return () => {
      controller.abort();
    };
  }, [accessToken, categoryId, logout, page, reloadKey, search, status]);

  return (
    <AppShell>
      <div className="space-y-8">
        <PageHeader
          title="Documentos"
          description="Gerencie e acompanhe os documentos da sua organização."
          action={
            <Button asChild>
              <Link to="/documents/new">
                <Plus />
                Novo documento
              </Link>
            </Button>
          }
        />

        <section className="rounded-xl border border-border bg-card p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <SlidersHorizontal className="size-4 text-primary" />
            <h2 className="font-semibold text-foreground">
              Pesquisa e filtros
            </h2>
          </div>

          <form
            aria-label="Pesquisar e filtrar documentos"
            onSubmit={applySearch}
            className="grid gap-4 lg:grid-cols-[minmax(16rem,1fr)_minmax(12rem,0.6fr)_minmax(12rem,0.5fr)_auto]"
          >
            <div className="space-y-2">
              <Label htmlFor="document-search">Título ou código</Label>
              <Input
                id="document-search"
                type="search"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Ex.: Política ou DOC-000001"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="document-category-filter">Categoria</Label>
              <select
                id="document-category-filter"
                value={categoryId}
                onChange={(event) => {
                  setCategoryId(event.target.value);
                  setPage(0);
                }}
                disabled={!categories || Boolean(categoriesError)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <option value="">Todas</option>
                {categories?.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="document-status-filter">Status</Label>
              <select
                id="document-status-filter"
                value={status}
                onChange={(event) => {
                  setStatus(event.target.value as DocumentStatus | "");
                  setPage(0);
                }}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30"
              >
                <option value="">Todos</option>
                {Object.entries(DOCUMENT_STATUS_LABELS).map(
                  ([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ),
                )}
              </select>
            </div>

            <div className="flex items-end gap-2">
              <Button type="submit">
                <Search />
                Buscar
              </Button>
              <Button
                type="button"
                variant="outline"
                aria-label="Limpar campos de pesquisa e filtros"
                disabled={!hasActiveFilters && !searchInput}
                onClick={clearFilters}
              >
                <X />
              </Button>
            </div>
          </form>

          {categoriesError ? (
            <div
              role="alert"
              className="mt-4 rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"
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
                onClick={() =>
                  setCategoriesReloadKey((current) => current + 1)
                }
              >
                Tentar novamente
              </Button>
            </div>
          ) : null}
        </section>

        <section
          aria-busy={isLoading}
          aria-live="polite"
        >
          {isLoading && !documents ? (
            <DocumentListSkeleton />
          ) : null}

          {!isLoading && error ? (
            <ApiErrorState
              title="Não foi possível carregar os documentos"
              message={error.message}
              correlationId={error.correlationId}
              onRetry={retry}
            />
          ) : null}

          {!isLoading && !error && documents?.items.length === 0 ? (
            <EmptyState
              icon={<FileText className="size-5" />}
              title="Nenhum documento encontrado"
              description={
                hasActiveFilters
                  ? "Nenhum documento visível corresponde aos critérios informados."
                  : "Quando um documento estiver disponível para o seu perfil, ele aparecerá aqui."
              }
              action={
                hasActiveFilters ? (
                  <Button variant="outline" onClick={clearFilters}>
                    Limpar filtros
                  </Button>
                ) : (
                  <Button asChild>
                    <Link to="/documents/new">
                      <Plus />
                      Criar documento
                    </Link>
                  </Button>
                )
              }
            />
          ) : null}

          {!error && documents && documents.items.length > 0 ? (
            <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-left">
                  <thead className="border-b border-border bg-muted/40">
                    <tr>
                      <th
                        scope="col"
                        className="px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                      >
                        Documento
                      </th>
                      <th
                        scope="col"
                        className="px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                      >
                        Status
                      </th>
                      <th
                        scope="col"
                        className="px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                      >
                        Categoria
                      </th>
                      <th
                        scope="col"
                        className="px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                      >
                        Atualizado em
                      </th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-border">
                    {documents.items.map((document) => (
                      <tr
                        key={document.id}
                        className="transition-colors hover:bg-muted/30"
                      >
                        <td className="max-w-xl px-5 py-4">
                          <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-primary">
                            {document.documentCode}
                          </p>
                          <Link
                            to={`/documents/${document.id}`}
                            className="font-medium text-foreground hover:text-primary hover:underline"
                          >
                            {document.title}
                          </Link>
                          <p className="mt-1 line-clamp-1 text-sm text-muted-foreground">
                            {document.description || "Sem descrição"}
                          </p>
                        </td>
                        <td className="px-5 py-4">
                          <DocumentStatusBadge status={document.status} />
                        </td>
                        <td className="px-5 py-4 text-sm text-muted-foreground">
                          {document.category.name}
                        </td>
                        <td className="whitespace-nowrap px-5 py-4 text-sm text-muted-foreground">
                          {formatDateTime(document.updatedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="flex flex-col gap-3 border-t border-border px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-muted-foreground">
                  {documents.totalElements === 1
                    ? "1 documento"
                    : `${documents.totalElements} documentos`}
                </p>

                <nav
                  aria-label="Paginação de documentos"
                  className="flex items-center gap-3"
                >
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={documents.page === 0 || isLoading}
                    onClick={() =>
                      setPage((current) => Math.max(0, current - 1))
                    }
                  >
                    <ChevronLeft />
                    Anterior
                  </Button>

                  <span className="min-w-24 text-center text-sm text-muted-foreground">
                    Página {documents.page + 1} de {documents.totalPages}
                  </span>

                  <Button
                    variant="outline"
                    size="sm"
                    disabled={
                      documents.page + 1 >= documents.totalPages ||
                      isLoading
                    }
                    onClick={() => setPage((current) => current + 1)}
                  >
                    Próxima
                    <ChevronRight />
                  </Button>
                </nav>
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </AppShell>
  );
}

function DocumentListSkeleton() {
  return (
    <div
      role="status"
      className="overflow-hidden rounded-xl border border-border bg-card"
    >
      <span className="sr-only">Carregando documentos</span>

      {Array.from({ length: 4 }, (_, index) => (
        <div
          key={index}
          className="flex animate-pulse items-center gap-6 border-b border-border px-5 py-5 last:border-b-0"
        >
          <div className="min-w-0 flex-1 space-y-2">
            <div className="h-4 w-48 rounded bg-muted" />
            <div className="h-3 w-72 max-w-full rounded bg-muted" />
          </div>
          <div className="h-6 w-24 rounded-full bg-muted" />
          <div className="hidden h-4 w-28 rounded bg-muted sm:block" />
        </div>
      ))}
    </div>
  );
}
