import { Download, Paperclip, Upload } from "lucide-react";
import {
  useEffect,
  useState,
  type FormEvent,
} from "react";

import { ApiError } from "@/api/client";
import {
  downloadDocumentFile,
  listDocumentFiles,
  uploadDocumentFile,
} from "@/api/documents";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatDateTime } from "@/lib/date";
import type { DocumentFile } from "@/types/document";

interface FileFeedback {
  message: string;
  correlationId?: string;
}

export function DocumentAttachments({
  documentId,
  accessToken,
  canUpload,
  onUnauthorized,
  onNotice,
}: {
  documentId: string;
  accessToken: string | null;
  canUpload: boolean;
  onUnauthorized: () => void;
  onNotice: (message: string) => void;
}) {
  const [files, setFiles] = useState<DocumentFile[] | null>(null);
  const [error, setError] = useState<FileFeedback | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    const controller = new AbortController();

    async function loadFiles() {
      setError(null);

      try {
        setFiles(
          await listDocumentFiles({
            accessToken: token,
            documentId,
            signal: controller.signal,
          }),
        );
      } catch (caughtError) {
        if (controller.signal.aborted) {
          return;
        }

        if (caughtError instanceof ApiError && caughtError.status === 401) {
          onUnauthorized();
          return;
        }

        setError({
          message:
            caughtError instanceof ApiError && caughtError.status === 404
              ? "O documento não foi encontrado ou não está disponível para o seu perfil."
              : "Não foi possível carregar os anexos.",
          correlationId:
            caughtError instanceof ApiError
              ? caughtError.correlationId
              : undefined,
        });
      }
    }

    void loadFiles();

    return () => controller.abort();
  }, [accessToken, documentId, onUnauthorized, reloadKey]);

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedFile) {
      setError({ message: "Selecione um arquivo para enviar." });
      return;
    }

    if (!accessToken) {
      onUnauthorized();
      return;
    }

    setIsUploading(true);
    setError(null);

    try {
      await uploadDocumentFile({
        accessToken,
        documentId,
        file: selectedFile,
      });
      setSelectedFile(null);
      setFileInputKey((current) => current + 1);
      onNotice("Arquivo anexado com sucesso.");
      setReloadKey((current) => current + 1);
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 401) {
        onUnauthorized();
        return;
      }

      setError({
        message: uploadErrorMessage(caughtError),
        correlationId:
          caughtError instanceof ApiError
            ? caughtError.correlationId
            : undefined,
      });
    } finally {
      setIsUploading(false);
    }
  }

  async function handleDownload(file: DocumentFile) {
    if (!accessToken) {
      onUnauthorized();
      return;
    }

    setDownloadingId(file.id);
    setError(null);

    try {
      const blob = await downloadDocumentFile({
        accessToken,
        documentId,
        fileId: file.id,
      });
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement("a");

      link.href = url;
      link.download = file.originalFilename;
      link.click();
      URL.revokeObjectURL(url);
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 401) {
        onUnauthorized();
        return;
      }

      setError({
        message:
          caughtError instanceof ApiError && caughtError.status === 404
            ? "O arquivo não foi encontrado ou não está disponível."
            : "Não foi possível baixar o arquivo.",
        correlationId:
          caughtError instanceof ApiError
            ? caughtError.correlationId
            : undefined,
      });
    } finally {
      setDownloadingId(null);
    }
  }

  return (
    <section
      className="rounded-xl border border-border bg-card p-6 shadow-sm"
      aria-labelledby="attachments-heading"
    >
      <div className="flex items-start gap-3">
        <Paperclip className="mt-0.5 text-primary" />
        <div>
          <h2 id="attachments-heading" className="font-semibold text-foreground">
            Anexos
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Arquivos PDF, TXT, PNG ou JPEG, com até 10 MB.
          </p>
        </div>
      </div>

      {canUpload ? (
        <form
          className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-end"
          onSubmit={(event) => void handleUpload(event)}
          aria-label="Anexar arquivo"
        >
          <div className="min-w-0 flex-1 space-y-2">
            <Label htmlFor="document-file">Arquivo</Label>
            <Input
              key={fileInputKey}
              id="document-file"
              type="file"
              accept=".pdf,.txt,.png,.jpg,.jpeg"
              onChange={(event) =>
                setSelectedFile(event.target.files?.[0] ?? null)
              }
            />
          </div>
          <Button type="submit" disabled={isUploading}>
            <Upload />
            {isUploading ? "Enviando..." : "Anexar"}
          </Button>
        </form>
      ) : null}

      {error ? (
        <div
          role="alert"
          className="mt-4 rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3"
        >
          <p className="text-sm text-destructive">{error.message}</p>
          {error.correlationId ? (
            <p className="mt-1 text-xs text-muted-foreground">
              Código de rastreio: {error.correlationId}
            </p>
          ) : null}
          {files === null ? (
            <Button
              className="mt-3"
              variant="outline"
              onClick={() => setReloadKey((current) => current + 1)}
            >
              Tentar novamente
            </Button>
          ) : null}
        </div>
      ) : null}

      {files === null && !error ? (
        <p className="mt-5 text-sm text-muted-foreground">
          Carregando anexos...
        </p>
      ) : files?.length === 0 ? (
        <p className="mt-5 text-sm text-muted-foreground">
          Nenhum arquivo anexado.
        </p>
      ) : (
        <ul className="mt-5 divide-y divide-border">
          {files?.map((file) => (
            <li
              key={file.id}
              className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-foreground">
                  {file.originalFilename}
                </p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {formatFileSize(file.size)} · {formatDateTime(file.uploadedAt)}
                </p>
              </div>
              <Button
                variant="outline"
                disabled={downloadingId === file.id}
                onClick={() => void handleDownload(file)}
              >
                <Download />
                {downloadingId === file.id ? "Baixando..." : "Baixar"}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function uploadErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return "Não foi possível anexar o arquivo.";
  }

  if (error.status === 400) {
    return error.message;
  }

  if (error.status === 403) {
    return "Este documento não permite novos anexos.";
  }

  if (error.status === 404) {
    return "O documento não foi encontrado ou não está disponível para o seu perfil.";
  }

  return "Não foi possível anexar o arquivo.";
}

function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toLocaleString("pt-BR", {
      maximumFractionDigits: 1,
    })} KB`;
  }

  return `${(size / (1024 * 1024)).toLocaleString("pt-BR", {
    maximumFractionDigits: 1,
  })} MB`;
}
