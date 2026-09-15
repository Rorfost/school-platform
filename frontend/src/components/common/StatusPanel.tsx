import type { ReactNode } from "react";
import { AlertCircle, FolderOpen, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { LABELS } from "@/utils/gujarati";

export function LoadingState({ message = LABELS.loading }: { message?: string }) {
  return (
    <div
      className="flex items-center justify-center gap-3 rounded-xl border border-slate-200 bg-white p-8 text-slate-700 shadow-xs"
      role="status"
      aria-live="polite"
    >
      <Spinner className="size-5" />
      <span className="text-sm sm:text-base font-medium">{message}</span>
    </div>
  );
}

export function ErrorState({
  message = LABELS.errorFallback,
  onRetry,
  children,
}: {
  message?: string;
  onRetry?: () => void;
  children?: ReactNode;
}) {
  return (
    <div
      className="rounded-xl border border-red-200 bg-red-50/70 p-6 text-red-950 shadow-xs"
      role="alert"
    >
      <div className="flex items-start gap-3.5">
        <AlertCircle className="mt-0.5 shrink-0 text-red-600" aria-hidden="true" size={22} />
        <div className="flex-1">
          <p className="text-sm sm:text-base font-medium leading-relaxed">{children ?? message}</p>
          {onRetry && (
            <div className="mt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={onRetry}
                className="border-red-300 text-red-800 hover:bg-red-100 hover:text-red-900"
              >
                <RefreshCw size={14} aria-hidden="true" />
                <span>{LABELS.tryAgain}</span>
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function EmptyState({
  title = LABELS.empty,
  description,
  action,
  icon: Icon = FolderOpen,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
  icon?: typeof FolderOpen;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center shadow-xs">
      <div className="flex size-12 items-center justify-center rounded-full bg-slate-100 text-slate-500 mb-3.5">
        <Icon size={24} aria-hidden="true" />
      </div>
      <h3 className="text-base font-semibold text-slate-800">{title}</h3>
      {description && (
        <p className="mt-1.5 max-w-sm text-sm text-slate-500 leading-relaxed">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
