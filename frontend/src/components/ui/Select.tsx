import type { SelectHTMLAttributes } from "react";
import { useId } from "react";

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export function Select({
  label,
  error,
  helperText,
  id,
  className = "",
  children,
  ...rest
}: SelectProps) {
  const generatedId = useId();
  const selectId = id || generatedId;
  const errorId = `${selectId}-error`;
  const helperId = `${selectId}-helper`;

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={selectId} className="block text-sm font-medium text-slate-700 mb-1.5">
          {label}
        </label>
      )}
      <select
        id={selectId}
        aria-invalid={!!error}
        aria-describedby={error ? errorId : helperText ? helperId : undefined}
        className={`w-full min-h-11 rounded-lg border py-2 pl-3.5 pr-10 text-sm text-slate-900 bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 focus-visible:border-transparent transition-colors disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed ${
          error
            ? "border-red-500 focus-visible:ring-red-600"
            : "border-slate-300 hover:border-slate-400"
        } ${className}`}
        {...rest}
      >
        {children}
      </select>
      {error ? (
        <p id={errorId} className="mt-1.5 text-xs font-medium text-red-600" role="alert">
          {error}
        </p>
      ) : helperText ? (
        <p id={helperId} className="mt-1.5 text-xs text-slate-500">
          {helperId}
        </p>
      ) : null}
    </div>
  );
}
