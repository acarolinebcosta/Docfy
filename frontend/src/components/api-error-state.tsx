import { CircleAlert, RefreshCw } from "lucide-react";
import type { ReactNode } from "react";

import { EmptyState } from "@/components/empty-state";
import { Button } from "@/components/ui/button";

export function ApiErrorState({
  title,
  message,
  correlationId,
  onRetry,
  secondaryAction,
}: {
  title: string;
  message: string;
  correlationId?: string;
  onRetry?: () => void;
  secondaryAction?: ReactNode;
}) {
  return (
    <EmptyState
      icon={<CircleAlert className="size-5" />}
      title={title}
      description={message}
      action={
        onRetry || secondaryAction || correlationId ? (
          <div className="space-y-3">
            <div className="flex flex-wrap justify-center gap-2">
              {onRetry ? (
                <Button onClick={onRetry}>
                  <RefreshCw />
                  Tentar novamente
                </Button>
              ) : null}

              {secondaryAction}
            </div>

            {correlationId ? (
              <p className="text-xs text-muted-foreground">
                Código de rastreio: {correlationId}
              </p>
            ) : null}
          </div>
        ) : undefined
      }
    />
  );
}
