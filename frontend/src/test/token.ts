import type { Role } from "@/types/auth";

export function createTestToken({
  id = "11111111-1111-1111-1111-111111111111",
  email = "ana@docfy.local",
  role = "COLLABORATOR",
  expiresAt = Date.now() + 3_600_000,
}: {
  id?: string;
  email?: string;
  role?: Role;
  expiresAt?: number;
} = {}): string {
  const header = encode({ alg: "HS256", typ: "JWT" });
  const payload = encode({
    sub: id,
    email,
    role,
    exp: Math.floor(expiresAt / 1000),
  });

  return `${header}.${payload}.test-signature`;
}

function encode(value: object): string {
  return btoa(JSON.stringify(value))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}
