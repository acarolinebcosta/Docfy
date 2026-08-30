interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  correlationId?: string;
}

interface ApiRequestOptions extends RequestInit {
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
  const {
    accessToken,
    headers: providedHeaders,
    ...requestOptions
  } = options;

  const headers = new Headers(providedHeaders);

  if (requestOptions.body !== undefined && !headers.has("Content-Type")) {
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

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}