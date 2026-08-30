import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { login as loginRequest } from "@/api/auth";
import { AuthContext } from "@/auth/auth-context";
import { readUserFromToken } from "@/auth/session";
import type { LoginCredentials } from "@/types/auth";

const STORAGE_KEY = "docfy.accessToken";

function restoreAccessToken(): string | null {
  const token = sessionStorage.getItem(STORAGE_KEY);

  if (!token) {
    return null;
  }

  if (!readUserFromToken(token)) {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }

  return token;
}

export function AuthProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [accessToken, setAccessToken] = useState<
    string | null
  >(() => restoreAccessToken());

  const user = useMemo(
    () =>
      accessToken
        ? readUserFromToken(accessToken)
        : null,
    [accessToken],
  );

  const logout = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setAccessToken(null);
  }, []);

  const login = useCallback(
    async (credentials: LoginCredentials) => {
      const response = await loginRequest(credentials);

      const authenticatedUser = readUserFromToken(
        response.accessToken,
      );

      if (!authenticatedUser) {
        throw new Error(
          "O servidor retornou um token de autenticação inválido.",
        );
      }

      sessionStorage.setItem(
        STORAGE_KEY,
        response.accessToken,
      );

      setAccessToken(response.accessToken);
    },
    [],
  );

  useEffect(() => {
    if (!user) {
      return;
    }

    const remainingTime =
      user.expiresAt - Date.now();

    const timeout = window.setTimeout(
      logout,
      Math.max(0, remainingTime),
    );

    return () => {
      window.clearTimeout(timeout);
    };
  }, [logout, user]);

  const value = useMemo(
    () => ({
      user,
      accessToken,
      isAuthenticated: Boolean(
        user && accessToken,
      ),
      login,
      logout,
    }),
    [
      user,
      accessToken,
      login,
      logout,
    ],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}