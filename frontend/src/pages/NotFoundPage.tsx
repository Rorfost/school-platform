import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";
import { LABELS } from "@/utils/gujarati";

export function NotFoundPage() {
  return (
    <section className="mx-auto max-w-xl py-10 sm:py-16 text-center">
      <p className="text-sm font-semibold text-blue-900 tracking-wide">૪૦૪ ભૂલ</p>
      <h1 className="mt-2 text-2xl sm:text-3xl font-bold text-slate-900">
        પાનું મળ્યું નથી.
      </h1>
      <p className="mt-3 text-sm sm:text-base text-slate-600 max-w-md mx-auto leading-relaxed">
        તમે જે પાનું શોધી રહ્યા છો તે ખસેડવામાં આવ્યું છે અથવા ઉપલબ્ધ નથી.
      </p>
      <div className="mt-6">
        <Link
          to="/"
          className="inline-flex items-center gap-2 rounded-lg bg-blue-900 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 focus-visible:ring-offset-2"
        >
          <ArrowLeft size={16} aria-hidden="true" />
          <span>{LABELS.home} પર પાછા જાઓ</span>
        </Link>
      </div>
    </section>
  );
}
