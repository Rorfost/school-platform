import { useState } from "react";
import { AdminExamResultsTab } from "./AdminExamResultsTab";
import { AdminResultPresentationSettings } from "./AdminResultPresentationSettings";

export type ResultSettingsTab = "ANNUAL" | "EKAM_KASOTI";

const TABS: { id: ResultSettingsTab; label: string }[] = [
  { id: "ANNUAL", label: "Exam Result" },
  { id: "EKAM_KASOTI", label: "Trimasik Kasoti Result" },
];

export function AdminAssessmentsPage() {
  const [activeTab, setActiveTab] = useState<ResultSettingsTab>("ANNUAL");

  return (
    <section className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">
            Exam &amp; Trimasik Kasoti Results
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Choose Exam or Trimasik Kasoti, then upload its workbook and save matching
          result-sheet details.
        </p>
      </div>
      <div
        role="tablist"
        aria-label="Result type"
        className="flex gap-1 rounded-xl border border-slate-200 bg-slate-50 p-1"
      >
        {TABS.map((tab) => {
          const selected = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              role="tab"
              id={`result-tab-${tab.id}`}
              aria-selected={selected}
              aria-controls={`result-panel-${tab.id}`}
              onClick={() => setActiveTab(tab.id)}
              className={`min-h-11 flex-1 rounded-lg px-3 text-sm font-semibold ${
                selected
                  ? "bg-white text-slate-900 shadow-sm"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>
      <div
        role="tabpanel"
        id={`result-panel-${activeTab}`}
        aria-labelledby={`result-tab-${activeTab}`}
        className="space-y-6"
      >
        <AdminExamResultsTab key={activeTab} resultType={activeTab} />
        <AdminResultPresentationSettings settingsTab={activeTab} />
      </div>
    </section>
  );
}
