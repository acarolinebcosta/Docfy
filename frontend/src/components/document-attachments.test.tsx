import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, delay, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";

import { DocumentAttachments } from "@/components/document-attachments";
import { formatDateTime } from "@/lib/date";
import { server } from "@/test/server";
import type { DocumentFile } from "@/types/document";

const DOCUMENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const FILE_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const FILE_FIXTURE: DocumentFile = {
  id: FILE_ID,
  documentId: DOCUMENT_ID,
  originalFilename: "quality-evidence.txt",
  contentType: "text/plain",
  size: 2048,
  uploadedBy: "cccccccc-cccc-cccc-cccc-cccccccccccc",
  uploadedAt: "2026-08-31T12:00:00Z",
};

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("DocumentAttachments", () => {
  it("shows loading and then an empty state", async () => {
    server.use(
      http.get("*/api/v1/documents/:id/files", async () => {
        await delay(30);
        return HttpResponse.json([]);
      }),
    );

    renderAttachments();

    expect(screen.getByText("Carregando anexos...")).toBeInTheDocument();
    expect(
      await screen.findByText("Nenhum arquivo anexado."),
    ).toBeInTheDocument();
  });

  it("lists real metadata with a readable size and date", async () => {
    server.use(
      http.get("*/api/v1/documents/:id/files", () =>
        HttpResponse.json([FILE_FIXTURE]),
      ),
    );

    renderAttachments();

    expect(
      await screen.findByText(FILE_FIXTURE.originalFilename),
    ).toBeInTheDocument();
    expect(screen.getByText(/2 KB/)).toBeInTheDocument();
    expect(
      screen.getByText(
        new RegExp(formatDateTime(FILE_FIXTURE.uploadedAt)),
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Baixar" }),
    ).toBeInTheDocument();
  });

  it("only exposes upload controls when editing is allowed", async () => {
    const { rerender } = renderAttachments({ canUpload: false });

    await screen.findByText("Nenhum arquivo anexado.");
    expect(screen.queryByLabelText("Arquivo")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Anexar" }),
    ).not.toBeInTheDocument();

    rerender(
      attachmentElement({
        canUpload: true,
        onUnauthorized: vi.fn(),
        onNotice: vi.fn(),
      }),
    );

    expect(screen.getByLabelText("Arquivo")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Anexar" }),
    ).toBeInTheDocument();
  });

  it("uploads multipart data and refreshes the list", async () => {
    let uploaded = false;
    let submittedContentType: string | null = null;
    const onNotice = vi.fn();

    server.use(
      http.get("*/api/v1/documents/:id/files", () =>
        HttpResponse.json(uploaded ? [FILE_FIXTURE] : []),
      ),
      http.post("*/api/v1/documents/:id/files", ({ request }) => {
        submittedContentType = request.headers.get("content-type");
        uploaded = true;
        return HttpResponse.json(FILE_FIXTURE, { status: 201 });
      }),
    );

    const user = userEvent.setup();
    renderAttachments({ onNotice });
    await screen.findByText("Nenhum arquivo anexado.");

    await user.upload(
      screen.getByLabelText("Arquivo"),
      new File(["Quality evidence"], "quality-evidence.txt", {
        type: "text/plain",
      }),
    );
    await user.click(screen.getByRole("button", { name: "Anexar" }));

    expect(
      await screen.findByText(FILE_FIXTURE.originalFilename),
    ).toBeInTheDocument();
    expect(submittedContentType).toMatch(
      /^multipart\/form-data; boundary=/,
    );
    expect(onNotice).toHaveBeenCalledWith("Arquivo anexado com sucesso.");
  });

  it.each([
    [403, "Este documento não permite novos anexos."],
    [404, "O documento não foi encontrado ou não está disponível para o seu perfil."],
  ])("shows a safe upload error for HTTP %s", async (status, message) => {
    server.use(
      http.post("*/api/v1/documents/:id/files", () =>
        HttpResponse.json(
          {
            status,
            message: "Internal authorization detail",
            correlationId: `upload-${status}`,
          },
          { status },
        ),
      ),
    );

    const user = userEvent.setup();
    renderAttachments();
    await screen.findByText("Nenhum arquivo anexado.");
    await user.upload(
      screen.getByLabelText("Arquivo"),
      new File(["evidence"], "evidence.txt", { type: "text/plain" }),
    );
    await user.click(screen.getByRole("button", { name: "Anexar" }));

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(screen.getByText(`Código de rastreio: upload-${status}`))
      .toBeInTheDocument();
    expect(
      screen.queryByText("Internal authorization detail"),
    ).not.toBeInTheDocument();
  });

  it("preserves a structured 400 message and correlation ID", async () => {
    server.use(
      http.post("*/api/v1/documents/:id/files", () =>
        HttpResponse.json(
          {
            status: 400,
            message: "File type is not allowed",
            correlationId: "invalid-file-correlation",
          },
          { status: 400 },
        ),
      ),
    );

    const user = userEvent.setup();
    renderAttachments();
    await screen.findByText("Nenhum arquivo anexado.");
    await user.upload(
      screen.getByLabelText("Arquivo"),
      new File(["unsafe"], "unsafe.txt", {
        type: "text/plain",
      }),
    );
    await user.click(screen.getByRole("button", { name: "Anexar" }));

    expect(
      await screen.findByText("File type is not allowed"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Código de rastreio: invalid-file-correlation"),
    ).toBeInTheDocument();
  });

  it("downloads through the authenticated API", async () => {
    let downloadRequests = 0;
    server.use(
      http.get("*/api/v1/documents/:id/files", () =>
        HttpResponse.json([FILE_FIXTURE]),
      ),
      http.get("*/api/v1/documents/:id/files/:fileId", () => {
        downloadRequests += 1;
        return new HttpResponse("Quality evidence", {
          headers: { "Content-Type": "text/plain" },
        });
      }),
    );
    const objectUrl = vi.fn(() => "blob:docfy-file");
    const revokeUrl = vi.fn();
    vi.spyOn(URL, "createObjectURL").mockImplementation(objectUrl);
    vi.spyOn(URL, "revokeObjectURL").mockImplementation(revokeUrl);
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    const user = userEvent.setup();
    renderAttachments();
    await user.click(await screen.findByRole("button", { name: "Baixar" }));

    await waitFor(() => expect(downloadRequests).toBe(1));
    expect(objectUrl).toHaveBeenCalled();
    expect(revokeUrl).toHaveBeenCalledWith("blob:docfy-file");
  });

  it("ends the session when attachment loading returns 401", async () => {
    server.use(
      http.get("*/api/v1/documents/:id/files", () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );
    const onUnauthorized = vi.fn();

    renderAttachments({ onUnauthorized });

    await waitFor(() => expect(onUnauthorized).toHaveBeenCalledOnce());
  });

  it("shows correlation ID and retries a failed list request", async () => {
    let requests = 0;
    server.use(
      http.get("*/api/v1/documents/:id/files", () => {
        requests += 1;
        return requests === 1
          ? HttpResponse.json(
              {
                status: 500,
                message: "Internal error",
                correlationId: "files-retry-correlation",
              },
              { status: 500 },
            )
          : HttpResponse.json([]);
      }),
    );

    const user = userEvent.setup();
    renderAttachments();

    expect(
      await screen.findByText("Não foi possível carregar os anexos."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Código de rastreio: files-retry-correlation"),
    ).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );

    expect(
      await screen.findByText("Nenhum arquivo anexado."),
    ).toBeInTheDocument();
    expect(requests).toBe(2);
  });
});

function renderAttachments({
  canUpload = true,
  onUnauthorized = vi.fn(),
  onNotice = vi.fn(),
}: {
  canUpload?: boolean;
  onUnauthorized?: () => void;
  onNotice?: (message: string) => void;
} = {}) {
  return render(
    attachmentElement({ canUpload, onUnauthorized, onNotice }),
  );
}

function attachmentElement({
  canUpload,
  onUnauthorized,
  onNotice,
}: {
  canUpload: boolean;
  onUnauthorized: () => void;
  onNotice: (message: string) => void;
}) {
  return (
    <DocumentAttachments
      documentId={DOCUMENT_ID}
      accessToken="test-access-token"
      canUpload={canUpload}
      onUnauthorized={onUnauthorized}
      onNotice={onNotice}
    />
  );
}
