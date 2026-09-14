import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="mx-auto max-w-xl py-8 sm:py-14">
      <h1 className="text-3xl font-semibold">પાનું મળ્યું નથી.</h1>
      <p className="mt-4 text-slate-700">તમે જે પાનું શોધી રહ્યા છો તે ઉપલબ્ધ નથી.</p>
      <Link
        className="mt-6 inline-flex rounded-md bg-emerald-900 px-4 py-2.5 font-medium text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2"
        to="/"
      >
        મુખપૃષ્ઠ પર જાઓ
      </Link>
    </section>
  );
}
