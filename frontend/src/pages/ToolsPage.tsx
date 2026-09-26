import { PageHeader } from "@/components/common/PageHeader";
import { AgeCalculator } from "@/features/tools/AgeCalculator";
import { AttendanceCalculator } from "@/features/tools/AttendanceCalculator";
import { DateDifferenceCalculator } from "@/features/tools/DateDifferenceCalculator";
import { MarksCalculator } from "@/features/tools/MarksCalculator";
import { MultiSubjectMarksCalculator } from "@/features/tools/MultiSubjectMarksCalculator";
import { LABELS } from "@/utils/gujarati";

export function ToolsPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 sm:space-y-8">
      <PageHeader
        title={LABELS.tools}
        description="શિક્ષકો, વાલીઓ અને બાળકો માટેની સરળ ગણતરીઓ. દાખલ કરેલી કોઈ માહિતી સંગ્રહિત થતી નથી."
        backTo="/student"
        backLabel={LABELS.studentCorner}
      />
      <div className="grid gap-6 lg:grid-cols-2">
        <AgeCalculator />
        <DateDifferenceCalculator />
        <AttendanceCalculator />
        <MarksCalculator />
        <MultiSubjectMarksCalculator />
      </div>
    </div>
  );
}
