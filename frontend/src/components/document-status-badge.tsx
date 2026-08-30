import type { DocumentStatus } from "@/types/document";
import { cn } from "@/lib/utils";
import { DOCUMENT_STATUS_LABELS } from "@/lib/document-status";

const STATUS_STYLES: Record<DocumentStatus, string> = {
  DRAFT: "bg-muted text-muted-foreground",
  IN_REVIEW: "bg-warning/10 text-warning",
  APPROVED: "bg-success/10 text-success",
  ARCHIVED: "bg-secondary text-secondary-foreground",
};

export function DocumentStatusBadge({
  status,
}: {
  status: DocumentStatus;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium",
        STATUS_STYLES[status],
      )}
    >
      {DOCUMENT_STATUS_LABELS[status]}
    </span>
  );
}
