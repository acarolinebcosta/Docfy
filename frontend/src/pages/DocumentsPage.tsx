import {
  ChevronLeft,
  ChevronRight,
  FileText,
  Plus,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useState,
} from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/client";
import { listDocuments } from "@/api/documents";
import { useAuth } from "@/auth/useAuth";
import { ApiErrorState } from "@/components/api-error-state";
import { AppShell } from "@/components/app-shell";
import { DocumentStatusBadge } from "@/components/document-status-badge";
import { EmptyState } from "@/components/empty-state";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { formatDateTime } from "@/lib/date";
import type { DocumentPage } from "@/types/document";

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
  }, [accessToken, logout, page, reloadKey]);

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
              description="Quando um documento estiver disponível para o seu perfil, ele aparecerá aqui."
              action={
                <Button asChild>
                  <Link to="/documents/new">
                    <Plus />
                    Criar documento
                  </Link>
                </Button>
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
