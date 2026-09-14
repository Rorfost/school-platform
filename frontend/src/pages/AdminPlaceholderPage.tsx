import { LockKeyhole } from "lucide-react";

export function AdminPlaceholderPage() {
  return (
    <section className="mx-auto max-w-xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <LockKeyhole className="text-emerald-800" aria-hidden="true" size={28} />
      <h1 className="mt-4 text-2xl font-semibold">વ્યવસ્થાપન વિભાગ</h1>
      <p className="mt-3 leading-7 text-slate-700">
        સુરક્ષિત વ્યવસ્થાપન પ્રવેશની ગોઠવણ હજી શરૂ કરવામાં આવી નથી.
      </p>
    </section>
  );
}
