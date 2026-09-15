import type { ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";
import { LABELS } from "@/utils/gujarati";

export interface PageHeaderProps {
  title: string;
  description?: string;
  backTo?: string;
  backLabel?: string;
  action?: ReactNode;
}

export function PageHeader({
  title,
  description,
  backTo,
  backLabel = LABELS.back,
  action,
}: PageHeaderProps) {
  return (
    <div className="mb-6 border-b border-slate-200 pb-5 sm:mb-8 sm:pb-6">
      {backTo && (
        <Link
          to={backTo}
          className="mb-3 inline-flex items-center gap-1.5 text-xs sm:text-sm font-medium text-slate-600 hover:text-blue-900 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 rounded-sm"
        >
          <ArrowLeft size={16} aria-hidden="true" />
          <span>{backLabel}</span>
        </Link>
      )}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            {title}
          </h1>
          {description && (
            <p className="mt-1.5 text-sm sm:text-base text-slate-600 max-w-2xl leading-relaxed">
              {description}
            </p>
          )}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
    </div>
  );
}
