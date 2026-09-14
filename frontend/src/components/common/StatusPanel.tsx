import type { ReactNode } from "react";
import { AlertCircle } from "lucide-react";
import { Spinner } from "@/components/ui/Spinner";

export function LoadingState() {
  return (
    <div
      className="flex items-center gap-3 rounded-lg border border-slate-200 bg-white p-5 text-slate-700"
      role="status"
    >
      <Spinner />
      <span>માહિતી લોડ થઈ રહી છે.</span>
    </div>
  );
}

export function ErrorState({
  children = "હમણાં માહિતી મેળવી શકાઈ નથી. કૃપા કરીને ફરી પ્રયાસ કરો.",
}: {
  children?: ReactNode;
}) {
  return (
    <div
      className="flex gap-3 rounded-lg border border-red-200 bg-red-50 p-5 text-red-950"
      role="alert"
    >
      <AlertCircle className="mt-0.5 shrink-0" aria-hidden="true" size={20} />
      <p>{children}</p>
    </div>
  );
}
