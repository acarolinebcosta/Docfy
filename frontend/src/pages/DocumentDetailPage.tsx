import {
  Archive,
  ArrowLeft,
  Check,
  Clock3,
  Edit3,
  History,
  RotateCcw,
  Send,
  UserRound,
  X,
} from "lucide-react";
import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import {
  Link,
  useLocation,
  useParams,
} from "react-router-dom";

import { ApiError } from "@/api/client";
import {
  getDocument,
  getDocumentAudit,
  runDocumentWorkflow,
  updateDocument,
} from "@/api/documents";
import { useAuth } from "@/auth/useAuth";
import { ApiErrorState } from "@/components/api-error-state";
import { AppShell } from "@/components/app-shell";
import { DocumentStatusBadge } from "@/components/document-status-badge";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatDateTime } from "@/lib/date";
import {
  availableWorkflowActions,
  canEditDocument,
  canViewDocumentAudit,
} from "@/lib/document-permissions";
import { DOCUMENT_STATUS_LABELS } from "@/lib/document-status";
import type {
  Document,
  DocumentAuditAction,
  DocumentAuditEvent,
  DocumentWorkflowAction,
  UpdateDocumentInput,
} from "@/types/document";

interface Feedback {
  message: string;
  correlationId?: string;
}

const WORKFLOW_LABELS: Record<DocumentWorkflowAction, string> = {
  submit: "Enviar para revisão",
  approve: "Aprovar",
  reject: "Rejeitar",
  archive: "Arquivar",
};

const AUDIT_ACTION_LABELS: Record<DocumentAuditAction, string> = {
  DOCUMENT_SUBMITTED: "Enviado para revisão",
  DOCUMENT_APPROVED: "Documento aprovado",
  DOCUMENT_REJECTED: "Revisão rejeitada",
  DOCUMENT_ARCHIVED: "Documento arquivado",
};

export function DocumentDetailPage() {
  const { id: documentId } = useParams();

  if (!documentId) {
    return (
      <AppShell>
        <ApiErrorState
          title="Documento inválido"
          message="O identificador do documento não foi informado."
          secondaryAction={backToDocumentsButton()}
        />
      </AppShell>
    );
  }

  return (
    <DocumentDetailContent
      key={documentId}
      documentId={documentId}
    />
  );
}

function DocumentDetailContent({
  documentId,
}: {
  documentId: string;
}) {
  const location = useLocation();
  const { user, accessToken, logout } = useAuth();

  const [document, setDocument] = useState<Document | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<Feedback | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [notice, setNotice] = useState<string | null>(() => {
    const state = location.state as { notice?: string } | null;
    return state?.notice ?? null;
  });
  const [actionError, setActionError] = useState<Feedback | null>(null);
  const [pendingAction, setPendingAction] =
    useState<DocumentWorkflowAction | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [auditEvents, setAuditEvents] =
    useState<DocumentAuditEvent[] | null>(null);
  const [auditError, setAuditError] = useState<Feedback | null>(null);
  const [auditReloadKey, setAuditReloadKey] = useState(0);

  useEffect(() => {
    if (!accessToken || !documentId) {
      return;
    }

    const token = accessToken;
    const id = documentId;
    const controller = new AbortController();

    async function loadDocument() {
      setIsLoading(true);
      setLoadError(null);

      try {
        const response = await getDocument({
          accessToken: token,
          documentId: id,
          signal: controller.signal,
        });

        setDocument(response);
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

        setLoadError({
          message:
            caughtError instanceof ApiError && caughtError.status === 404
              ? "O documento não foi encontrado ou não está disponível para o seu perfil."
              : "Não foi possível carregar o documento.",
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

    void loadDocument();

    return () => controller.abort();
  }, [accessToken, documentId, logout, reloadKey]);

  const canViewAudit = Boolean(
    user && canViewDocumentAudit(user),
  );

  useEffect(() => {
    if (!accessToken || !document || !canViewAudit) {
      return;
    }

    const token = accessToken;
    const id = document.id;
    const controller = new AbortController();

    async function loadAudit() {
      setAuditError(null);

      try {
        const response = await getDocumentAudit({
          accessToken: token,
          documentId: id,
          signal: controller.signal,
        });

        setAuditEvents(response);
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

        setAuditError({
          message: "Não foi possível carregar o histórico do documento.",
          correlationId:
            caughtError instanceof ApiError
              ? caughtError.correlationId
              : undefined,
        });
      }
    }

    void loadAudit();

    return () => controller.abort();
  }, [accessToken, auditReloadKey, canViewAudit, document, logout]);

  const workflowActions = useMemo(
    () =>
      document && user
        ? availableWorkflowActions(document, user)
        : [],
    [document, user],
  );

  const canEdit = Boolean(
    document && user && canEditDocument(document, user),
  );

  async function handleWorkflow(action: DocumentWorkflowAction) {
    if (isEditing) {
      return;
    }

    if (!accessToken || !document) {
      logout();
      return;
    }

    if (
      action === "archive" &&
      !window.confirm(
        "Arquivar este documento? Essa ação encerra o fluxo atual.",
      )
    ) {
      return;
    }

    setPendingAction(action);
    setActionError(null);
    setNotice(null);

    try {
      const updated = await runDocumentWorkflow({
        accessToken,
        documentId: document.id,
        action,
      });

      setDocument(updated);
      setIsEditing(false);
      setNotice(`${WORKFLOW_LABELS[action]} concluído com sucesso.`);
      setAuditReloadKey((current) => current + 1);
    } catch (caughtError) {
      if (
        caughtError instanceof ApiError &&
        caughtError.status === 401
      ) {
        logout();
        return;
      }

      setActionError({
        message: workflowErrorMessage(caughtError),
        correlationId:
          caughtError instanceof ApiError
            ? caughtError.correlationId
            : undefined,
      });
    } finally {
      setPendingAction(null);
    }
  }

  function handleUpdated(updated: Document) {
    setDocument(updated);
    setIsEditing(false);
    setActionError(null);
    setNotice("Documento atualizado com sucesso.");
  }

  return (
    <AppShell>
      <div className="space-y-8">
        <PageHeader
          title={document?.title ?? "Documento"}
          description="Consulte os metadados, o fluxo e o histórico deste documento."
          action={backToDocumentsButton()}
        />

        {isLoading && !document ? <DocumentDetailSkeleton /> : null}

        {!isLoading && loadError ? (
          <ApiErrorState
            title="Documento indisponível"
            message={loadError.message}
            correlationId={loadError.correlationId}
            onRetry={() => setReloadKey((current) => current + 1)}
            secondaryAction={backToDocumentsButton()}
          />
        ) : null}

        {document ? (
          <>
            {notice ? (
              <div
                role="status"
                className="rounded-lg border border-success/20 bg-success/5 px-4 py-3 text-sm text-success"
              >
                {notice}
              </div>
            ) : null}

            {actionError ? (
              <div
                role="alert"
                className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"
              >
                <p className="text-sm text-destructive">
                  {actionError.message}
                </p>
                {actionError.correlationId ? (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Código de rastreio: {actionError.correlationId}
                  </p>
                ) : null}
              </div>
            ) : null}

            <section className="rounded-xl border border-border bg-card p-6 shadow-sm">
              <div className="flex flex-col gap-5 border-b border-border pb-6 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-3">
                    <h2 className="text-xl font-semibold text-foreground">
                      {document.title}
                    </h2>
                    <DocumentStatusBadge status={document.status} />
                  </div>
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-muted-foreground">
                    {document.description || "Este documento não possui descrição."}
                  </p>
                </div>

                {canEdit && !isEditing ? (
                  <Button
                    variant="outline"
                    onClick={() => setIsEditing(true)}
                  >
                    <Edit3 />
                    Editar
                  </Button>
                ) : null}
              </div>

              <dl className="grid gap-5 pt-6 sm:grid-cols-3">
                <MetadataItem
                  icon={<UserRound />}
                  label="Proprietário"
                  value={
                    document.createdBy === user?.id
                      ? "Você"
                      : "Outro usuário"
                  }
                />
                <MetadataItem
                  icon={<Clock3 />}
                  label="Criado em"
                  value={formatDateTime(document.createdAt)}
                />
                <MetadataItem
                  icon={<Clock3 />}
                  label="Atualizado em"
                  value={formatDateTime(document.updatedAt)}
                />
              </dl>
            </section>

            {isEditing && canEdit ? (
              <DocumentEditForm
                document={document}
                accessToken={accessToken}
                onUnauthorized={logout}
                onCancel={() => setIsEditing(false)}
                onUpdated={handleUpdated}
              />
            ) : null}

            <section className="rounded-xl border border-border bg-card p-6 shadow-sm">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="font-semibold text-foreground">
                    Fluxo do documento
                  </h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Estado atual: {DOCUMENT_STATUS_LABELS[document.status]}.
                  </p>
                </div>

                {isEditing ? (
                  <p className="text-sm text-muted-foreground">
                    Conclua ou cancele a edição para executar uma ação de fluxo.
                  </p>
                ) : workflowActions.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {workflowActions.map((action) => (
                      <WorkflowButton
                        key={action}
                        action={action}
                        isPending={pendingAction === action}
                        disabled={pendingAction !== null}
                        onClick={() => void handleWorkflow(action)}
                      />
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Nenhuma ação disponível para seu perfil neste estado.
                  </p>
                )}
              </div>
            </section>

            {canViewAudit ? (
              <AuditTrail
                events={auditEvents}
                error={auditError}
                onRetry={() =>
                  setAuditReloadKey((current) => current + 1)
                }
              />
            ) : null}
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

function DocumentEditForm({
  document,
  accessToken,
  onUnauthorized,
  onCancel,
  onUpdated,
}: {
  document: Document;
  accessToken: string | null;
  onUnauthorized: () => void;
  onCancel: () => void;
  onUpdated: (document: Document) => void;
}) {
  const [title, setTitle] = useState(document.title);
  const [description, setDescription] = useState(
    document.description ?? "",
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<Feedback | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedTitle = title.trim();
    const normalizedDescription = description.trim() || null;

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

    const changes: UpdateDocumentInput = {};

    if (normalizedTitle !== document.title) {
      changes.title = normalizedTitle;
    }

    if (normalizedDescription !== document.description) {
      changes.description = normalizedDescription;
    }

    if (Object.keys(changes).length === 0) {
      setError({ message: "Nenhuma alteração foi informada." });
      return;
    }

    if (!accessToken) {
      onUnauthorized();
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      const updated = await updateDocument({
        accessToken,
        documentId: document.id,
        changes,
      });

      onUpdated(updated);
    } catch (caughtError) {
      if (
        caughtError instanceof ApiError &&
        caughtError.status === 401
      ) {
        onUnauthorized();
        return;
      }

      setError({
        message:
          caughtError instanceof ApiError && caughtError.status === 403
            ? "Este documento não pode mais ser editado."
            : caughtError instanceof ApiError && caughtError.status === 400
              ? "Revise os dados informados."
              : "Não foi possível atualizar o documento.",
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
    <form
      aria-label="Editar documento"
      onSubmit={handleSubmit}
      className="space-y-5 rounded-xl border border-primary/20 bg-card p-6 shadow-sm"
    >
      <div>
        <h2 className="font-semibold text-foreground">
          Editar metadados
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Apenas documentos em rascunho podem ser alterados.
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="edit-document-title">Título</Label>
        <Input
          id="edit-document-title"
          required
          maxLength={255}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="edit-document-description">Descrição</Label>
        <Textarea
          id="edit-document-description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
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

      <div className="flex flex-wrap justify-end gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={isSubmitting}
          onClick={onCancel}
        >
          <X />
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          <Check />
          {isSubmitting ? "Salvando..." : "Salvar alterações"}
        </Button>
      </div>
    </form>
  );
}

function WorkflowButton({
  action,
  isPending,
  disabled,
  onClick,
}: {
  action: DocumentWorkflowAction;
  isPending: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  const icons = {
    submit: <Send />,
    approve: <Check />,
    reject: <RotateCcw />,
    archive: <Archive />,
  };

  return (
    <Button
      variant={action === "reject" ? "outline" : "default"}
      disabled={disabled}
      onClick={onClick}
    >
      {icons[action]}
      {isPending ? "Processando..." : WORKFLOW_LABELS[action]}
    </Button>
  );
}

function AuditTrail({
  events,
  error,
  onRetry,
}: {
  events: DocumentAuditEvent[] | null;
  error: Feedback | null;
  onRetry: () => void;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-6 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
          <History className="size-5" />
        </div>
        <div>
          <h2 className="font-semibold text-foreground">Audit Trail</h2>
          <p className="text-sm text-muted-foreground">
            Histórico cronológico das mudanças de estado.
          </p>
        </div>
      </div>

      {error ? (
        <div className="mt-6 rounded-lg border border-destructive/30 bg-destructive/5 p-4">
          <p className="text-sm text-destructive">{error.message}</p>
          {error.correlationId ? (
            <p className="mt-1 text-xs text-muted-foreground">
              Código de rastreio: {error.correlationId}
            </p>
          ) : null}
          <Button
            variant="outline"
            size="sm"
            className="mt-3"
            onClick={onRetry}
          >
            Tentar novamente
          </Button>
        </div>
      ) : null}

      {!error && events === null ? (
        <p className="mt-6 text-sm text-muted-foreground">
          Carregando histórico...
        </p>
      ) : null}

      {!error && events?.length === 0 ? (
        <p className="mt-6 rounded-lg bg-muted/50 p-4 text-sm text-muted-foreground">
          Nenhuma mudança de estado foi registrada.
        </p>
      ) : null}

      {!error && events && events.length > 0 ? (
        <ol className="mt-6 space-y-5 border-l border-border pl-5">
          {events.map((event) => (
            <li key={event.id} className="relative">
              <span className="absolute -left-[1.57rem] top-1.5 size-2.5 rounded-full bg-primary ring-4 ring-card" />
              <p className="text-sm font-medium text-foreground">
                {AUDIT_ACTION_LABELS[event.action]}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {DOCUMENT_STATUS_LABELS[event.previousStatus]} →{" "}
                {DOCUMENT_STATUS_LABELS[event.newStatus]}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {formatDateTime(event.occurredAt)} · Ator {event.actorId}
              </p>
              {event.correlationId ? (
                <p className="mt-1 text-xs text-muted-foreground">
                  Correlação: {event.correlationId}
                </p>
              ) : null}
            </li>
          ))}
        </ol>
      ) : null}
    </section>
  );
}

function MetadataItem({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex gap-3">
      <div className="mt-0.5 text-muted-foreground [&_svg]:size-4">
        {icon}
      </div>
      <div>
        <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {label}
        </dt>
        <dd className="mt-1 text-sm text-foreground">{value}</dd>
      </div>
    </div>
  );
}

function DocumentDetailSkeleton() {
  return (
    <div
      role="status"
      className="animate-pulse space-y-4 rounded-xl border border-border bg-card p-6"
    >
      <span className="sr-only">Carregando documento</span>
      <div className="h-6 w-64 rounded bg-muted" />
      <div className="h-4 w-full rounded bg-muted" />
      <div className="h-4 w-2/3 rounded bg-muted" />
      <div className="grid gap-4 pt-6 sm:grid-cols-3">
        <div className="h-12 rounded bg-muted" />
        <div className="h-12 rounded bg-muted" />
        <div className="h-12 rounded bg-muted" />
      </div>
    </div>
  );
}

function backToDocumentsButton() {
  return (
    <Button variant="outline" asChild>
      <Link to="/documents">
        <ArrowLeft />
        Voltar aos documentos
      </Link>
    </Button>
  );
}

function workflowErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return "Não foi possível concluir a ação.";
  }

  if (error.status === 403) {
    return "Seu perfil não pode executar esta ação.";
  }

  if (error.status === 404) {
    return "O documento não foi encontrado ou não está disponível.";
  }

  if (error.status === 409) {
    return "O estado do documento mudou. Recarregue a página e tente novamente.";
  }

  return "Não foi possível concluir a ação.";
}
