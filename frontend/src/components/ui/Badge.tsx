import type { ReactNode } from "react";

export interface BadgeProps {
  variant?: "primary" | "secondary" | "accent" | "neutral" | "success";
  size?: "sm" | "md";
  className?: string;
  children: ReactNode;
}

export function Badge({ variant = "primary", size = "md", className = "", children }: BadgeProps) {
  const variantStyles = {
    primary: "bg-blue-50 text-blue-900 border-blue-200",
    secondary: "bg-amber-50 text-amber-900 border-amber-200",
    accent: "bg-red-50 text-red-900 border-red-200",
    neutral: "bg-slate-100 text-slate-800 border-slate-200",
    success: "bg-emerald-50 text-emerald-900 border-emerald-200",
  }[variant];

  const sizeStyles = {
    sm: "px-2 py-0.5 text-xs",
    md: "px-2.5 py-1 text-xs sm:text-sm",
  }[size];

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${variantStyles} ${sizeStyles} ${className}`}
    >
      {children}
    </span>
  );
}
