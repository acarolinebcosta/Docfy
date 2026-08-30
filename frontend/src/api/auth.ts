import { apiRequest } from "@/api/client";
import type {
  LoginCredentials,
  LoginResponse,
} from "@/types/auth";

export function login(
  credentials: LoginCredentials,
): Promise<LoginResponse> {
  return apiRequest<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
}