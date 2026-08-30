import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import {
  afterAll,
  afterEach,
  beforeAll,
} from "vitest";

import { server } from "@/test/server";

const nativeFetch = globalThis.fetch.bind(globalThis);

beforeAll(() => {
  globalThis.fetch = (input, init) => {
    if (typeof input === "string") {
      return nativeFetch(new URL(input, window.location.origin), init);
    }

    return nativeFetch(input, init);
  };

  server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
  sessionStorage.clear();
});

afterAll(() => {
  server.close();
  globalThis.fetch = nativeFetch;
});
