import { cn } from "@/lib/utils";

export function DocfyMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      className={cn("size-8", className)}
      aria-hidden="true"
    >
      <rect width="32" height="32" rx="8" fill="#125E57" />
      <path
        fill="#FFFFFF"
        d="M10 8c0-1.105.895-2 2-2h5l6 6v12c0 1.105-.895 2-2 2H12c-1.105 0-2-.895-2-2V8z"
      />
      <path fill="#C8E0DB" d="M17 6l6 6V6z" />
      <rect x="12" y="16" width="8" height="2" rx="1" fill="#125E57" />
      <rect x="12" y="20" width="5" height="2" rx="1" fill="#125E57" />
    </svg>
  );
}

export function DocfyLogo({
  className,
  inverted = false,
}: {
  className?: string;
  inverted?: boolean;
}) {
  return (
    <div className={cn("flex items-center gap-2.5", className)}>
      <DocfyMark />
      <span
        className={cn(
          "text-lg font-semibold tracking-tight",
          inverted ? "text-primary-foreground" : "text-foreground",
        )}
      >
        Docfy
      </span>
    </div>
  );
}
