export type Role = "ADMIN" | "MANAGER" | "COLLABORATOR";

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface AuthUser {
  id: string;
  email: string;
  role: Role;
  expiresAt: number;
}