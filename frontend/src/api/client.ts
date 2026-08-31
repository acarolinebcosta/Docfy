interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  correlationId?: string;
}

export interface ApiRequestOptions extends RequestInit {
  accessToken?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly correlationId?: string;

  constructor(
    message: string,
    status: number,
    correlationId?: string,
  ) {
    super(message);

    this.name = "ApiError";
    this.status = status;
    this.correlationId = correlationId;
  }
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const response = await executeRequest(path, options);

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function apiBlobRequest(
  path: string,
  options: ApiRequestOptions = {},
): Promise<Blob> {
  const response = await executeRequest(path, options);

  return response.blob();
}

async function executeRequest(
  path: string,
  options: ApiRequestOptions,
): Promise<Response> {
  const {
    accessToken,
    headers: providedHeaders,
    ...requestOptions
  } = options;

  const headers = new Headers(providedHeaders);
  const body = requestOptions.body;
  const isFormData =
    typeof FormData !== "undefined" && body instanceof FormData;

  if (
    body !== undefined &&
    !isFormData &&
    typeof body === "string" &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(path, {
    ...requestOptions,
    headers,
  });

  if (!response.ok) {
    let body: ApiErrorBody | null = null;

    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      // Algumas respostas podem não possuir corpo JSON.
    }

    const correlationId =
      body?.correlationId ??
      response.headers.get("x-correlation-id") ??
      undefined;

    throw new ApiError(
      body?.message ?? "Não foi possível concluir a solicitação.",
      response.status,
      correlationId,
    );
  }

  return response;
}
