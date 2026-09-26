import { useState } from "react";
import { AdminEkamKasotiTab } from "./AdminEkamKasotiTab";
import { AdminExamResultsTab } from "./AdminExamResultsTab";

type ResultTab = "exam-results" | "ekam-kasoti";

export function AdminAssessmentsPage() {
  const [activeTab, setActiveTab] = useState<ResultTab>("exam-results");

  return (
    <section className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">પરિણામ અને ગુણ</h1>
        <p className="mt-1 text-sm text-slate-600">પરિણામ માટે યોગ્ય એક્સેલ ફાઇલ પસંદ કરીને અપલોડ કરો.</p>
      </div>

      <div className="flex border-b border-slate-200" role="tablist" aria-label="પરિણામ પ્રકાર">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "exam-results"}
          onClick={() => setActiveTab("exam-results")}
          className={`min-h-11 border-b-2 px-4 text-sm font-semibold transition-colors ${
            activeTab === "exam-results"
              ? "border-[#0d2461] text-[#0d2461]"
              : "border-transparent text-slate-600 hover:text-[#0d2461]"
          }`}
        >
          પરીક્ષા પરિણામ
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === "ekam-kasoti"}
          onClick={() => setActiveTab("ekam-kasoti")}
          className={`min-h-11 border-b-2 px-4 text-sm font-semibold transition-colors ${
            activeTab === "ekam-kasoti"
              ? "border-[#0d2461] text-[#0d2461]"
              : "border-transparent text-slate-600 hover:text-[#0d2461]"
          }`}
        >
          એકમ કસોટી પરિણામ (અપલોડ)
        </button>
      </div>

      {activeTab === "exam-results" ? <AdminExamResultsTab /> : <AdminEkamKasotiTab />}
    </section>
  );
}
