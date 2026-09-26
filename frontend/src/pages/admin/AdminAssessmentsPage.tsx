import { AdminExamResultsTab } from "./AdminExamResultsTab";
import { AdminResultPresentationSettings } from "./AdminResultPresentationSettings";

export function AdminAssessmentsPage() {
  return (
    <section className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Exam &amp; Ekam Kasoti Results</h1>
        <p className="mt-1 text-sm text-slate-600">
          Select the result type, then upload its Excel workbook.
        </p>
      </div>
      <AdminExamResultsTab />
      <AdminResultPresentationSettings />
    </section>
  );
}
