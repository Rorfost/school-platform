import { Printer } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { Button } from "@/components/ui/Button";
import type { ExamResultResponse, ResultPresentationSettingsResponse } from "@/api/types";
import { formatDate } from "@/utils/date";
import schoolLogo from "@/assets/school-logo.jpeg";

export function ExamResultViewer({
  result,
  schoolName,
  logoUrl,
  resultType = "ANNUAL",
}: {
  result: ExamResultResponse;
  schoolName: string;
  logoUrl?: string | null;
  resultType?: "ANNUAL" | "EKAM_KASOTI";
}) {
  const { data: settings } = useQuery<ResultPresentationSettingsResponse>({
    queryKey: ["result-settings", result.standard],
    queryFn: () =>
      apiRequest<ResultPresentationSettingsResponse>(
        `/api/v1/public/result-settings?standard=${encodeURIComponent(result.standard)}`,
      ),
  });
  const handlePrint = () => window.print();
  const isEkam = resultType === "EKAM_KASOTI";
  const resultTypeLabel = isEkam ? "ત્રિમાસિક કસોટી" : "વાર્ષિક પરીક્ષા";
  const sheetTitle = isEkam
    ? settings?.ekamResultSheetTitle || `${resultTypeLabel} પરિણામ પત્રક`
    : settings?.resultSheetTitle || `${resultTypeLabel} પરિણામ પત્રક`;
  const resultDateValue = isEkam ? settings?.ekamResultDate : settings?.resultDate;

  return (
    <div className="space-y-4">
      <div className="flex justify-end print:hidden">
        <Button onClick={handlePrint} variant="primary" className="gap-2">
          <Printer size={16} /> Print Result
        </Button>
      </div>

      <div
        id="exam-result-print"
        className="exam-result-sheet mx-auto bg-white p-1.5 sm:p-4 text-slate-900 print:p-0 print:m-0 print:w-full print:shadow-none font-sans"
        style={{ maxWidth: "210mm" }}
      >
        <div className="border-2 sm:border-4 border-slate-900 p-0.5 sm:p-1 print:border-4 print:p-1">
          <div className="border border-slate-900 sm:border-2 print:border-2">
            {/* Header */}
            <div className="flex items-center border-b-2 border-slate-900">
              <div className="w-[70px] sm:w-[110px] shrink-0 p-1 sm:p-2 border-r-2 border-slate-900 flex justify-center items-center print:w-[110px] print:p-2">
                <img
                  src={logoUrl || schoolLogo}
                  alt="School Logo"
                  className="size-12 sm:size-20 object-contain print:size-20"
                  crossOrigin="anonymous"
                />
              </div>
              <div className="flex-1 text-center py-2 px-1 flex flex-col justify-center leading-tight">
                <h1 className="text-sm sm:text-2xl font-bold text-blue-900 tracking-wide print:text-2xl">
                  {schoolName}
                </h1>
                <p className="text-xs sm:text-base font-bold text-blue-900 mt-0.5 sm:mt-1 print:text-base">
                  તા. સમી, જિ. પાટણ
                </p>
                <div className="mt-1.5 sm:mt-2 text-red-700 font-bold border-t-2 border-red-700 mx-auto w-11/12 sm:w-3/4 pt-1 text-xs sm:text-base print:text-base">
                  {sheetTitle}
                </div>
              </div>
            </div>

            {/* Student Info – responsive table layout */}
            <div className="border-b-2 border-slate-900 text-xs sm:text-sm font-semibold print:text-sm">
              <div className="grid grid-cols-12">
                <div className="col-span-12 sm:col-span-6 border-b sm:border-b-0 sm:border-r-2 border-slate-900 p-1.5 px-2 sm:px-3 flex gap-1.5 sm:gap-2 items-center min-w-0 print:col-span-7 print:border-b-0 print:border-r-2">
                  <span className="font-bold text-blue-900 shrink-0">વિદ્યાર્થીનું નામ :</span>
                  <span className="font-bold truncate">{result.studentName}</span>
                </div>
                <div className="col-span-6 sm:col-span-3 border-r-2 border-slate-900 p-1.5 px-2 sm:px-3 flex gap-1.5 sm:gap-2 items-center min-w-0 print:col-span-3">
                  <span className="font-bold shrink-0">ધોરણ :</span>
                  <span>{result.standard}</span>
                </div>
                <div className="col-span-6 sm:col-span-3 p-1.5 px-2 sm:px-3 flex gap-1.5 sm:gap-2 items-center min-w-0 print:col-span-2">
                  <span className="font-bold shrink-0">વર્ગ :</span>
                  <span>-</span>
                </div>
              </div>
            </div>

            <div className="border-b-2 border-slate-900 text-xs sm:text-sm font-semibold print:text-sm">
              <div className="grid grid-cols-12">
                <div className="col-span-12 sm:col-span-6 border-b sm:border-b-0 sm:border-r-2 border-slate-900 p-1.5 px-2 sm:px-3 flex gap-1.5 sm:gap-2 items-center min-w-0 print:col-span-7 print:border-b-0 print:border-r-2">
                  <span className="font-bold shrink-0">જનરલ રજી.નંબર :</span>
                  <span>{result.generalRegisterNumber || "-"}</span>
                </div>
                <div className="col-span-7 sm:col-span-3 border-r-2 border-slate-900 p-1.5 px-1.5 sm:px-3 flex gap-1 sm:gap-2 items-center min-w-0 print:col-span-3 print:px-1.5">
                  <span className="font-bold shrink-0">જન્મ તારીખ :</span>
                  <span className="truncate">{formatDate(result.birthDate)}</span>
                </div>
                <div className="col-span-5 sm:col-span-3 p-1.5 px-1.5 sm:px-3 flex gap-1 sm:gap-2 items-center min-w-0 print:col-span-2">
                  <span className="font-bold shrink-0">રોલ નં :</span>
                  <span>{result.rollNumber}</span>
                </div>
              </div>
            </div>

            {!isEkam && (
              <div className="border-b-2 border-slate-900 px-2 py-1.5 text-[11px] font-semibold tracking-tight whitespace-nowrap sm:px-3 sm:text-sm print:px-3 print:text-sm">
                <span className="font-bold">
                  કુલ કાર્ય દિવસ : {result.totalWorkingDays ?? "-"} માંથી હાજર દિવસ{" "}
                  {result.attendedDays ?? "-"} છે.
                </span>
              </div>
            )}

            {/* Marks Table */}
            <div className="overflow-x-auto print:overflow-visible">
              <table className="w-full text-center text-xs sm:text-sm font-semibold border-collapse print:text-sm">
                <thead>
                  <tr className="bg-amber-100/90 text-slate-900">
                    <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-8 sm:w-12 font-bold print:p-2 print:w-12 whitespace-nowrap">
                      ક્રમ
                    </th>
                    <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 font-bold text-center print:p-2 whitespace-nowrap">
                      વિષય
                    </th>
                    <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-14 sm:w-24 font-bold print:p-2 print:w-24 whitespace-nowrap">
                      કુલ ગુણ
                    </th>
                    <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-16 sm:w-28 font-bold print:p-2 print:w-28 whitespace-nowrap">
                      મેળવેલ ગુણ
                    </th>
                    {!isEkam && (
                      <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-12 sm:w-20 font-bold print:p-2 print:w-20 whitespace-nowrap">
                        ગ્રેડ
                      </th>
                    )}
                    <th className="border-b-2 border-slate-900 p-1 sm:p-2 font-bold print:p-2 whitespace-nowrap">
                      વિષયના સંદર્ભમાં નોંધ
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {result.subjects.map((sub, idx) => (
                    <tr key={sub.id}>
                      <td className="border-b border-r-2 border-slate-900 p-1 sm:p-2 print:p-2">
                        {idx + 1}
                      </td>
                      <td className="border-b border-r-2 border-slate-900 p-1 sm:p-2 text-left pl-2 sm:pl-4 font-semibold print:p-2 print:pl-4">
                        {sub.subjectName}
                      </td>
                      <td className="border-b border-r-2 border-slate-900 p-1 sm:p-2 font-semibold print:p-2">
                        {sub.maximumMarks}
                      </td>
                      <td className="border-b border-r-2 border-slate-900 p-1 sm:p-2 font-semibold print:p-2">
                        {sub.status === "ABSENT" ? (
                          <span className="font-bold">AB</span>
                        ) : (
                          (sub.obtainedMarks ?? "-")
                        )}
                      </td>
                      {!isEkam && (
                        <td className="border-b border-r-2 border-slate-900 p-1 sm:p-2 font-bold print:p-2">
                          {sub.grade ?? "-"}
                        </td>
                      )}
                      <td className="border-b border-slate-900 p-1 sm:p-2 print:p-2"></td>
                    </tr>
                  ))}
                  <tr className="bg-amber-100/90 border-t-2 border-b-2 border-slate-900 text-slate-900">
                    <td
                      colSpan={2}
                      className="border-r-2 border-slate-900 p-1.5 sm:p-2.5 text-center font-extrabold text-xs sm:text-base print:text-base print:p-2.5"
                    >
                      {isEkam ? "મેળવેલ કુલ ગુણ" : "મેળવેલ કુલ ગુણ / ગ્રેડ"}
                    </td>
                    <td className="border-r-2 border-slate-900 p-1.5 sm:p-2.5 font-bold text-xs sm:text-base print:text-base print:p-2.5">
                      {result.totalMarks ?? "-"}
                    </td>
                    <td className="border-r-2 border-slate-900 p-1.5 sm:p-2.5 font-bold text-xs sm:text-base print:text-base print:p-2.5">
                      {result.obtainedMarks ?? "-"}
                    </td>
                    {!isEkam && (
                      <td className="border-r-2 border-slate-900 p-1.5 sm:p-2.5 font-extrabold text-xs sm:text-base print:text-base print:p-2.5">
                        {result.overallGrade ?? "-"}
                      </td>
                    )}
                    <td className="p-1.5 sm:p-2.5 text-center font-bold text-xs sm:text-base print:text-base print:p-2.5">
                      ટકા : &nbsp;&nbsp;&nbsp;{result.percentage ?? "-"} %
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            {/* Footer Date */}
            <div className="grid grid-cols-[auto_1fr] border-b-2 border-slate-900 text-xs sm:text-sm font-bold print:text-sm">
              <div className="p-2 px-3 sm:px-4 border-r-2 border-slate-900 print:px-4">
                પરિણામ તારીખ :
              </div>
              <div className="p-2 px-3 sm:px-4 print:px-4">
                {formatDate(resultDateValue || (isEkam ? null : "08/06/2026"))}
              </div>
            </div>

            {/* Signatures – flexbox layout for consistent alignment */}
            <div className="flex justify-between items-end border-b-2 border-slate-900 bg-white px-3 sm:px-8 pt-1 sm:pt-2 pb-2 min-h-0 print:px-8 print:pt-1 print:min-h-24">
              <div className="flex flex-col items-center justify-end min-w-[110px] sm:min-w-[140px] text-center gap-1 print:min-w-[140px]">
                {settings?.classTeacherSignatureUrl ? (
                  <img
                    src={settings.classTeacherSignatureUrl}
                    alt="Class teacher signature"
                    className="h-9 sm:h-12 max-w-28 sm:max-w-40 object-contain print:h-12 print:max-w-40"
                    crossOrigin="anonymous"
                  />
                ) : (
                  <div className="h-9 sm:h-12 print:h-12" aria-hidden="true" />
                )}
                <span className="font-bold text-xs sm:text-sm text-slate-900 print:text-sm whitespace-nowrap border-t border-slate-400 pt-1 px-2">
                  વર્ગ શિક્ષકની સહી
                </span>
              </div>
              <div className="flex flex-col items-center justify-end min-w-[110px] sm:min-w-[140px] text-center gap-1 print:min-w-[140px]">
                {settings?.principalSignatureUrl ? (
                  <img
                    src={settings.principalSignatureUrl}
                    alt="Principal seal and signature"
                    className="h-20 max-w-50 object-contain sm:h-24 sm:max-w-60 print:h-24 print:max-w-64"
                    crossOrigin="anonymous"
                  />
                ) : (
                  <div className="h-20 sm:h-24 print:h-24" aria-hidden="true" />
                )}
                <span className="font-bold text-xs sm:text-sm text-slate-900 print:text-sm whitespace-nowrap border-t border-slate-400 pt-1 px-2">
                  આચાર્યની સહી
                </span>
              </div>
            </div>

            {/* Note Lines */}
            <div className="bg-cyan-50/70 p-2 sm:p-3 text-xs sm:text-sm font-medium space-y-2 border-t border-slate-300 print:p-3 print:text-sm">
              {isEkam ? (
                <>
                  <p className="text-slate-900">{settings?.ekamFooterLineOne}</p>
                  {settings?.ekamFooterLineTwo ? (
                    <p className="text-center text-xs mt-2 text-slate-700">
                      {settings.ekamFooterLineTwo}
                    </p>
                  ) : null}
                </>
              ) : (
                <>
                  <p className="text-slate-900">{settings?.footerLineOne}</p>
                  <p className="text-center text-xs mt-2 text-slate-700">
                    {settings?.footerLineTwo}
                  </p>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
      <style>{`
        @media print {
          @page { size: A4 portrait; margin: 8mm; }
          html, body { margin: 0 !important; padding: 0 !important; background: white !important; }
          body * { visibility: hidden !important; }
          #exam-result-print, #exam-result-print * { visibility: visible !important; }
          #exam-result-print {
            position: absolute !important;
            inset: 0 auto auto 0 !important;
            max-width: none !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
            background: white !important;
            box-shadow: none !important;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
          }
        }
      `}</style>
    </div>
  );
}
