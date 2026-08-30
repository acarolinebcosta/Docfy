import { LogOut } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "@/auth/useAuth";
import { DocfyLogo } from "@/components/logo";
import { Button } from "@/components/ui/button";

export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();

  return (
    <main className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-4">
          <Link
            to="/documents"
            aria-label="Ir para documentos"
            className="rounded-md"
          >
            <DocfyLogo />
          </Link>

          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium text-foreground">
                {user?.email}
              </p>
              <p className="text-xs text-muted-foreground">
                {user?.role}
              </p>
            </div>

            <Button
              variant="outline"
              onClick={logout}
            >
              <LogOut />
              Sair
            </Button>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-6xl px-6 py-8">
        {children}
      </div>
    </main>
  );
}
