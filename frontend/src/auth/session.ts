import type {
  AuthUser,
  Role,
} from "@/types/auth";

interface JwtClaims {
  sub?: unknown;
  email?: unknown;
  role?: unknown;
  exp?: unknown;
}

const ROLES: readonly Role[] = [
  "ADMIN",
  "MANAGER",
  "COLLABORATOR",
];

function isRole(value: unknown): value is Role {
  return (
    typeof value === "string" &&
    ROLES.includes(value as Role)
  );
}

function decodeBase64Url(value: string): string {
  const base64 = value
    .replace(/-/g, "+")
    .replace(/_/g, "/");

  const padded = base64.padEnd(
    Math.ceil(base64.length / 4) * 4,
    "=",
  );

  const binary = atob(padded);

  const bytes = Uint8Array.from(
    binary,
    (character) => character.charCodeAt(0),
  );

  return new TextDecoder().decode(bytes);
}

export function readUserFromToken(
  token: string,
): AuthUser | null {
  const parts = token.split(".");

  if (parts.length !== 3) {
    return null;
  }

  try {
    const claims = JSON.parse(
      decodeBase64Url(parts[1]),
    ) as JwtClaims;

    if (
      typeof claims.sub !== "string" ||
      typeof claims.email !== "string" ||
      !isRole(claims.role) ||
      typeof claims.exp !== "number"
    ) {
      return null;
    }

    const expiresAt = claims.exp * 1000;

    if (expiresAt <= Date.now()) {
      return null;
    }

    return {
      id: claims.sub,
      email: claims.email,
      role: claims.role,
      expiresAt,
    };
  } catch {
    return null;
  }
}