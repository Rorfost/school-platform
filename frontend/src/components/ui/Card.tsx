import type { HTMLAttributes, ReactNode } from "react";

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  variant?: "default" | "interactive" | "accent";
}

export function Card({ children, variant = "default", className = "", ...rest }: CardProps) {
  const variantStyles = {
    default: "bg-white border-slate-200 shadow-xs",
    interactive:
      "bg-white border-slate-200 shadow-xs hover:shadow-md hover:border-blue-300 transition-all cursor-pointer",
    accent: "bg-blue-50/50 border-blue-200/80 shadow-xs",
  }[variant];

  return (
    <div className={`rounded-xl border p-4 sm:p-6 ${variantStyles} ${className}`} {...rest}>
      {children}
    </div>
  );
}

export function CardHeader({
  title,
  subtitle,
  action,
  className = "",
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div className={`flex items-start justify-between gap-4 mb-4 ${className}`}>
      <div>
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        {subtitle && <p className="text-sm text-slate-600 mt-1">{subtitle}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
