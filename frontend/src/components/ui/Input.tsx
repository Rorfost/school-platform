import type { InputHTMLAttributes } from "react";
import { useId } from "react";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  helperText?: string;
}

export function Input({
  label,
  error,
  helperText,
  id,
  className = "",
  ...rest
}: InputProps) {
  const generatedId = useId();
  const inputId = id || generatedId;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  return (
    <div className="w-full">
      <label
        htmlFor={inputId}
        className="block text-sm font-medium text-slate-700 mb-1.5"
      >
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={!!error}
        aria-describedby={error ? errorId : helperText ? helperId : undefined}
        className={`w-full min-h-11 rounded-lg border px-3.5 py-2 text-sm text-slate-900 bg-white placeholder:text-slate-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 focus-visible:border-transparent transition-colors ${
          error
            ? "border-red-500 focus-visible:ring-red-600"
            : "border-slate-300 hover:border-slate-400"
        } ${className}`}
        {...rest}
      />
      {error ? (
        <p id={errorId} className="mt-1.5 text-xs font-medium text-red-600" role="alert">
          {error}
        </p>
      ) : helperText ? (
        <p id={helperId} className="mt-1.5 text-xs text-slate-500">
          {helperText}
        </p>
      ) : null}
    </div>
  );
}
