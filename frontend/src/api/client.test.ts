import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import { apiRequest } from "@/api/client";
import { server } from "@/test/server";

describe("apiRequest content type", () => {
  it("uses application/json for JSON request bodies", async () => {
    server.use(
      http.post("*/api/test", ({ request }) =>
        HttpResponse.json({
          contentType: request.headers.get("content-type"),
        }),
      ),
    );

    const response = await apiRequest<{ contentType: string }>(
      "/api/test",
      {
        method: "POST",
        body: JSON.stringify({ title: "Quality Policy" }),
      },
    );

    expect(response.contentType).toBe("application/json");
  });

  it("lets the browser create the multipart boundary for FormData", async () => {
    server.use(
      http.post("*/api/test", ({ request }) =>
        HttpResponse.json({
          contentType: request.headers.get("content-type"),
        }),
      ),
    );

    const formData = new FormData();
    formData.append("file", new Blob(["document"]), "document.txt");

    const response = await apiRequest<{ contentType: string }>(
      "/api/test",
      {
        method: "POST",
        body: formData,
      },
    );

    expect(response.contentType).toMatch(
      /^multipart\/form-data; boundary=/,
    );
  });

  it("preserves a custom content type", async () => {
    server.use(
      http.post("*/api/test", ({ request }) =>
        HttpResponse.json({
          contentType: request.headers.get("content-type"),
        }),
      ),
    );

    const response = await apiRequest<{ contentType: string }>(
      "/api/test",
      {
        method: "POST",
        headers: { "Content-Type": "application/merge-patch+json" },
        body: JSON.stringify({ title: "Updated policy" }),
      },
    );

    expect(response.contentType).toBe(
      "application/merge-patch+json",
    );
  });
});
