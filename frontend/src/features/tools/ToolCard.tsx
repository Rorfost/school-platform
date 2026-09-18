import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { Card } from "@/components/ui/Card";

export function ToolCard({
  title,
  description,
  icon: Icon,
  children,
}: {
  title: string;
  description: string;
  icon: LucideIcon;
  children: ReactNode;
}) {
  return (
    <Card className="space-y-4" aria-labelledby={`${title}-heading`}>
      <div className="flex items-start gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
          <Icon size={20} aria-hidden="true" />
        </div>
        <div>
          <h2 id={`${title}-heading`} className="text-lg font-bold text-slate-900">
            {title}
          </h2>
          <p className="mt-1 text-sm leading-relaxed text-slate-600">{description}</p>
        </div>
      </div>
      {children}
    </Card>
  );
}

export function ToolResult({ children }: { children: ReactNode }) {
  return (
    <div
      className="rounded-lg border border-blue-200 bg-blue-50 p-4 text-sm text-blue-950"
      role="status"
      aria-live="polite"
    >
      {children}
    </div>
  );
}
