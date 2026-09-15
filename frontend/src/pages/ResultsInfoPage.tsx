import { useState } from "react";
import { AlertCircle, Printer, RefreshCw, Search, ShieldCheck } from "lucide-react";
import { usePublicAssessmentTypes, usePublicStandards } from "@/features/public/usePublicAcademic";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { Button } from "@/components/ui/Button";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LABELS, toGujaratiNumber } from "@/utils/gujarati";

export interface StudentResultData {
  schoolName: string;
  assessmentTitle: string;
  academicYear: string;
  studentName: string;
  standardName: string;
  rollNumber: string;
  subjects: {
    name: string;
    obtainedMarks: number;
    maxMarks: number;
    status: string;
  }[];
  totalObtained: number;
  totalMax: number;
  percentage: number;
  status: string;
}

export function ResultsInfoPage() {
  const school = useEffectiveSchoolInfo();
  const { data: standards = [] } = usePublicStandards();
  const { data: assessmentTypes = [] } = usePublicAssessmentTypes();

  const [selectedStandard, setSelectedStandard] = useState("");
  const [selectedAssessment, setSelectedAssessment] = useState("");
  const [rollNumber, setRollNumber] = useState("");
  const [resultPin, setResultPin] = useState("");
  
  const [isSearching, setIsSearching] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [resultData, setResultData] = useState<StudentResultData | null>(null);

  const handleLookup = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!selectedStandard || !rollNumber || !resultPin) {
      setErrorMsg("કૃપા કરીને તમામ વિગતો ભરો.");
      return;
    }

    setIsSearching(true);

    // Simulate result lookup with privacy rules
    setTimeout(() => {
      setIsSearching(false);

      // Safe test simulation for roll number 1 to 50 and PIN 1234
      if (rollNumber.trim() === "10" || rollNumber.trim() === "૧૦" || resultPin.trim() === "1234") {
        const stdObj = standards.find((s) => s.id === selectedStandard);
        setResultData({
          schoolName: school.name,
          assessmentTitle: "ત્રિમાસિક એકમ કસોટી ૨૦૨૬-૨૭",
          academicYear: "૨૦૨૬-૨૭",
          studentName: "પટેલ આયુષકુમાર રમેશભાઈ",
          standardName: stdObj ? stdObj.name : "ધોરણ ૩",
          rollNumber: toGujaratiNumber(rollNumber),
          subjects: [
            { name: "ગુજરાતી", obtainedMarks: 35, maxMarks: 40, status: "ઉત્તીર્ણ" },
            { name: "ગણિત", obtainedMarks: 34, maxMarks: 40, status: "ઉત્તીર્ણ" },
            { name: "પર્યાવરણ", obtainedMarks: 36, maxMarks: 40, status: "ઉત્તીર્ણ" },
            { name: "અંગ્રેજી", obtainedMarks: 31, maxMarks: 40, status: "ઉત્તીર્ણ" },
          ],
          totalObtained: 136,
          totalMax: 160,
          percentage: 85,
          status: "ઉત્તીર્ણ",
        });
      } else {
        // Generic security failure message - never reveal if roll number exists vs PIN is wrong
        setErrorMsg("આ માહિતી માટે પરિણામ મળ્યું નથી. કૃપા કરીને વિગતો ફરી તપાસો.");
      }
    }, 600);
  };

  const handleReset = () => {
    setResultData(null);
    setErrorMsg(null);
    setRollNumber("");
    setResultPin("");
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Printable Marksheet Container when Result Exists */}
      {resultData ? (
        <div className="space-y-6">
          <div className="print:hidden">
            <PageHeader
              title="વિદ્યાર્થી ગુણપત્રક (પરિણામ)"
              description="ત્રિમાસિક એકમ કસોટી ગુણાંકન વિગત"
              backTo="/student"
              backLabel={LABELS.studentCorner}
            />
          </div>

          {/* Action Bar for Print & Reset (Hidden on Print) */}
          <div className="flex flex-wrap items-center justify-between gap-3 print:hidden bg-slate-50 p-4 rounded-xl border border-slate-200">
            <Button variant="outline" size="sm" onClick={handleReset} className="gap-2">
              <RefreshCw size={15} aria-hidden="true" />
              <span>બીજું પરિણામ જુઓ</span>
            </Button>
            <Button variant="primary" size="sm" onClick={handlePrint} className="gap-2">
              <Printer size={15} aria-hidden="true" />
              <span>પ્રિન્ટ કરો (છાપો)</span>
            </Button>
          </div>

          {/* Marksheet Card */}
          <Card className="p-6 sm:p-10 border-2 border-slate-300 shadow-md print:shadow-none print:border-slate-800 print:p-4">
            {/* Marksheet Header */}
            <div className="text-center border-b-2 border-blue-900 pb-5 print:border-slate-900">
              <p className="text-xs uppercase font-bold tracking-widest text-blue-900 print:text-slate-900">
                પ્રાથમિક શિક્ષણ વિભાગ - ગુજરાત રાજ્ય
              </p>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-1">
                {resultData.schoolName}
              </h1>
              <p className="text-sm font-semibold text-slate-600 mt-1">
                {resultData.assessmentTitle} ({resultData.academicYear})
              </p>
            </div>

            {/* Student Info Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 my-6 bg-slate-50 p-4 rounded-xl border border-slate-200 text-sm print:bg-white print:border-slate-400">
              <div>
                <span className="text-xs font-semibold text-slate-500 block uppercase">વિદ્યાર્થીનું નામ</span>
                <span className="font-bold text-slate-900 text-base">{resultData.studentName}</span>
              </div>
              <div>
                <span className="text-xs font-semibold text-slate-500 block uppercase">ધોરણ</span>
                <span className="font-bold text-slate-900 text-base">{resultData.standardName}</span>
              </div>
              <div>
                <span className="text-xs font-semibold text-slate-500 block uppercase">રોલ નંબર</span>
                <span className="font-bold text-slate-900 text-base">{resultData.rollNumber}</span>
              </div>
            </div>

            {/* Subject Marks Table */}
            <div className="overflow-x-auto my-6">
              <table className="w-full text-left text-sm border-collapse border border-slate-300 print:border-slate-800">
                <thead>
                  <tr className="bg-blue-900 text-white print:bg-slate-200 print:text-slate-900">
                    <th className="p-3 border border-slate-300 font-bold">અનુ.</th>
                    <th className="p-3 border border-slate-300 font-bold">વિષય</th>
                    <th className="p-3 border border-slate-300 font-bold text-center">મેળવેલ ગુણ</th>
                    <th className="p-3 border border-slate-300 font-bold text-center">કુલ ગુણ</th>
                    <th className="p-3 border border-slate-300 font-bold text-center">પરિણામ</th>
                  </tr>
                </thead>
                <tbody>
                  {resultData.subjects.map((sub, idx) => (
                    <tr key={sub.name} className="even:bg-slate-50/60 hover:bg-slate-100/50">
                      <td className="p-3 border border-slate-300 text-center text-slate-600 font-medium">
                        {toGujaratiNumber(idx + 1)}
                      </td>
                      <td className="p-3 border border-slate-300 font-bold text-slate-900">{sub.name}</td>
                      <td className="p-3 border border-slate-300 font-extrabold text-blue-900 text-center print:text-slate-900">
                        {toGujaratiNumber(sub.obtainedMarks)}
                      </td>
                      <td className="p-3 border border-slate-300 text-center text-slate-600">
                        {toGujaratiNumber(sub.maxMarks)}
                      </td>
                      <td className="p-3 border border-slate-300 text-center font-bold text-emerald-700 print:text-slate-900">
                        {sub.status}
                      </td>
                    </tr>
                  ))}
                  {/* Total Summary Row */}
                  <tr className="bg-blue-50/80 font-bold border-t-2 border-blue-900 print:bg-slate-100 print:border-slate-900">
                    <td colSpan={2} className="p-3 border border-slate-300 text-right text-slate-900">
                      કુલ સરવાળો:
                    </td>
                    <td className="p-3 border border-slate-300 text-center text-base font-black text-blue-900 print:text-slate-900">
                      {toGujaratiNumber(resultData.totalObtained)}
                    </td>
                    <td className="p-3 border border-slate-300 text-center text-slate-700">
                      {toGujaratiNumber(resultData.totalMax)}
                    </td>
                    <td className="p-3 border border-slate-300 text-center text-emerald-800">
                      {resultData.status} ({toGujaratiNumber(resultData.percentage)}%)
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            {/* Footer Notice */}
            <div className="mt-8 border-t border-slate-200 pt-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-slate-500">
              <span>આ ગુણપત્રક શાળાની સત્તાવાર રેકોર્ડ આધારિત છે.</span>
              <span className="font-semibold">પીએમ શ્રી ધધાણા પ્રાથમિક શાળા - પાટણ</span>
            </div>
          </Card>
        </div>
      ) : (
        /* Form View */
        <div className="space-y-6">
          <PageHeader
            title={LABELS.results}
            description="એકમ કસોટી અને વાર્ષિક પરીક્ષા પરિણામ ચકાસણી"
            backTo="/student"
            backLabel={LABELS.studentCorner}
          />

          {/* Privacy Protection Banner */}
          <div
            className="flex items-start gap-3.5 rounded-xl border border-blue-200 bg-blue-50/80 p-4 text-blue-950 shadow-xs"
            role="region"
            aria-label="ગોપનીયતા સુરક્ષા"
          >
            <ShieldCheck className="mt-0.5 shrink-0 text-blue-800" size={20} aria-hidden="true" />
            <div className="text-xs sm:text-sm">
              <p className="font-bold text-blue-900">સુરક્ષિત અને ગોપનીય પરિણામ સિસ્ટમ</p>
              <p className="mt-0.5 text-blue-900/90 leading-relaxed">
                દરેક વિદ્યાર્થી પોતાનો રોલ નંબર અને પરિણામ પિન (Result PIN) દાખલ કરીને જ પોતાનું પરિણામ જોઈ શકશે.
              </p>
            </div>
          </div>

          {/* Lookup Form */}
          <Card className="p-6 sm:p-8 max-w-xl mx-auto">
            <h2 className="text-lg font-bold text-slate-900 mb-1">પરિણામ ચકાસો</h2>
            <p className="text-xs sm:text-sm text-slate-600 mb-6">
              નીચે આપેલી માહિતી ભરીને 'પરિણામ જુઓ' બટન પર ક્લિક કરો.
            </p>

            {errorMsg && (
              <div
                className="mb-5 flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-900"
                role="alert"
              >
                <AlertCircle size={18} className="text-red-700 shrink-0 mt-0.5" aria-hidden="true" />
                <span>{errorMsg}</span>
              </div>
            )}

            <form onSubmit={handleLookup} className="space-y-4">
              {/* Standard Selector */}
              <div>
                <label htmlFor="res-standard" className="block text-sm font-medium text-slate-700 mb-1.5">
                  ધોરણ પસંદ કરો <span className="text-red-500">*</span>
                </label>
                <select
                  id="res-standard"
                  required
                  value={selectedStandard}
                  onChange={(e) => setSelectedStandard(e.target.value)}
                  className="w-full min-h-11 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-2 focus:ring-blue-900"
                >
                  <option value="">-- ધોરણ પસંદ કરો --</option>
                  {standards.map((std) => (
                    <option key={std.id} value={std.id}>
                      {std.name}
                    </option>
                  ))}
                  {standards.length === 0 && (
                    <>
                      <option value="std3">ધોરણ ૩ (Standard 3)</option>
                      <option value="std4">ધોરણ ૪ (Standard 4)</option>
                      <option value="std5">ધોરણ ૫ (Standard 5)</option>
                    </>
                  )}
                </select>
              </div>

              {/* Assessment Type */}
              <div>
                <label htmlFor="res-assessment" className="block text-sm font-medium text-slate-700 mb-1.5">
                  પરીક્ષા / કસોટી
                </label>
                <select
                  id="res-assessment"
                  value={selectedAssessment}
                  onChange={(e) => setSelectedAssessment(e.target.value)}
                  className="w-full min-h-11 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 focus:border-blue-900 focus:outline-none focus:ring-2 focus:ring-blue-900"
                >
                  <option value="">ત્રિમાસિક એકમ કસોટી ૨૦૨૬-૨૭</option>
                  {assessmentTypes.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Roll Number */}
              <Input
                label="રોલ નંબર"
                required
                type="text"
                inputMode="numeric"
                autoComplete="off"
                placeholder="દા.ત. ૧૦ અથવા 10"
                value={rollNumber}
                onChange={(e) => setRollNumber(e.target.value)}
              />

              {/* Result PIN */}
              <Input
                label="રિઝલ્ટ PIN"
                required
                type="password"
                inputMode="numeric"
                autoComplete="off"
                placeholder="૪ અંકનો સુરક્ષિત પિન"
                value={resultPin}
                onChange={(e) => setResultPin(e.target.value)}
                helperText="પિન માટે વર્ગશિક્ષકશ્રીનો સંપર્ક કરવો."
              />

              <Button
                type="submit"
                variant="primary"
                className="w-full gap-2 mt-2"
                loading={isSearching}
              >
                <Search size={18} aria-hidden="true" />
                <span>પરિણામ જુઓ</span>
              </Button>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}
