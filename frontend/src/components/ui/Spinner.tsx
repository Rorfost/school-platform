export function Spinner({ className = "" }: { className?: string }) {
  return (
    <span
      aria-label="લોડ થઈ રહ્યું છે"
      className={`inline-block size-5 animate-spin rounded-full border-2 border-slate-300 border-t-blue-900 ${className}`}
      role="status"
    />
  );
}
