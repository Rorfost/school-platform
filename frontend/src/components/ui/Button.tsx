import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Spinner } from "@/components/ui/Spinner";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: "primary" | "secondary" | "outline" | "ghost" | "danger";
    size?: "sm" | "md" | "lg";
    loading?: boolean;
    loadingText?: ReactNode;
    children: ReactNode;
}

export function Button({
    variant = "primary",
    size = "md",
    loading = false,
    loadingText = "Loading...",
    disabled,
    className = "",
    children,
    ...rest
}: ButtonProps) {
    const variantStyles = {
        primary: "bg-brand-900 text-white hover:bg-brand-950 shadow-sm active:bg-brand-950",
        secondary: "bg-amber-600 text-white hover:bg-amber-700 shadow-sm active:bg-amber-800",
        outline:
            "border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 active:bg-slate-100",
        ghost: "bg-transparent text-slate-700 hover:bg-slate-100 active:bg-slate-200",
        danger: "bg-red-700 text-white hover:bg-red-800 active:bg-red-900",
    }[variant];

    const sizeStyles = {
        sm: "min-h-10 px-3 py-1.5 text-xs rounded-md",
        md: "min-h-11 px-4 py-2 text-sm rounded-lg",
        lg: "min-h-12 px-6 py-3 text-base rounded-lg",
    }[size];

    return (
        <button
            className={`inline-flex items-center justify-center gap-2 font-medium transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 ${variantStyles} ${sizeStyles} ${className}`}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            {...rest}
        >
            {loading ? (
                <>
                    <Spinner className="size-4 border-current border-t-transparent" />
                    <span>{loadingText}</span>
                </>
            ) : (
                children
            )}
        </button>
    );
}
