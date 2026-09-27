import { Printer } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { Button } from "@/components/ui/Button";
import type { ExamResultResponse, ResultPresentationSettingsResponse } from "@/api/types";
import { toGujaratiNumber } from "@/utils/gujarati";
import { formatDate } from "@/utils/date";
import schoolLogo from "@/assets/school-logo.jpeg";

export function ExamResultViewer({
  result,
  schoolName,
  logoUrl,
}: {
  result: ExamResultResponse;
  schoolName: string;
  logoUrl?: string | null;
}) {
  const { data: settings } = useQuery<ResultPresentationSettingsResponse>({
    queryKey: ["result-settings", result.standard],
    queryFn: () =>
      apiRequest<ResultPresentationSettingsResponse>(
        `/api/v1/public/result-settings?standard=${encodeURIComponent(result.standard)}`,
      ),
  });
  const handlePrint = () => window.print();

  return (
    <div className="space-y-4">
      <div className="flex justify-end print:hidden">
        <Button onClick={handlePrint} variant="primary" className="gap-2">
          <Printer size={16} /> Print Result
        </Button>
      </div>

      <div
        id="exam-result-print"
        className="mx-auto bg-white p-2 sm:p-4 text-slate-900 print:p-0 print:m-0 print:w-full print:shadow-none font-sans"
        style={{ maxWidth: "210mm" }}
      >
        <div className="border-4 border-slate-900 p-1">
          <div className="border-2 border-slate-900">
            {/* Header */}
            <div className="flex items-center border-b-2 border-slate-900">
              <div className="w-[110px] shrink-0 p-2 border-r-2 border-slate-900 flex justify-center items-center">
                <img
                  src={logoUrl || schoolLogo}
                  alt="School Logo"
                  className="size-20 object-contain"
                  crossOrigin="anonymous"
                />
              </div>
              <div className="flex-1 text-center py-2 flex flex-col justify-center leading-tight">
                <h1 className="text-xl sm:text-2xl font-bold text-blue-900 tracking-wide">
                  {schoolName}
                </h1>
                <p className="text-sm sm:text-base font-bold text-blue-900 mt-1">
                  તા. સમી, જિ. પાટણ
                </p>
                <div className="mt-2 text-red-700 font-bold border-t-2 border-red-700 mx-auto w-3/4 pt-1 text-sm sm:text-base">
                  {settings?.resultSheetTitle || "પરિણામ પત્રક : ૨૦૨૫-૨૬"}
                </div>
              </div>
            </div>

            {/* Student Info */}
            <div className="grid grid-cols-[1fr_auto_auto] border-b-2 border-slate-900 text-sm font-semibold">
              <div className="border-r-2 border-slate-900 p-1.5 px-3 flex gap-2">
                <span className="font-bold text-blue-900">વિદ્યાર્થીનું નામ :</span>
                <span className="font-bold">{result.studentName}</span>
              </div>
              <div className="border-r-2 border-slate-900 p-1.5 px-3 flex gap-2 w-32 sm:w-36">
                <span className="font-bold">ધોરણ :</span>
                <span>{toGujaratiNumber(result.standard)}</span>
              </div>
              <div className="p-1.5 px-3 flex gap-2 w-24">
                <span className="font-bold">વર્ગ :</span>
                <span>-</span>
              </div>
            </div>

            <div className="grid grid-cols-3 border-b-2 border-slate-900 text-sm font-semibold">
              <div className="p-1.5 px-3 border-r-2 border-slate-900 flex gap-2 items-center">
                <span className="font-bold">જનરલ રજી.નંબર :</span>
                <span>{result.generalRegisterNumber || "-"}</span>
              </div>
              <div className="p-1.5 px-3 border-r-2 border-slate-900 flex gap-2 items-center">
                <span className="font-bold">જન્મ તારીખ :</span>
                <span>{formatDate(result.birthDate)}</span>
              </div>
              <div className="p-1.5 px-3 flex gap-2 items-center">
                <span className="font-bold">રોલ નં :</span>
                <span>{result.rollNumber}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 border-b-2 border-slate-900 text-sm font-semibold">
              <div className="p-1.5 px-3 border-r-2 border-slate-900 flex gap-2 items-center">
                <span className="font-bold">કુલ કાર્ય દિવસ :</span>
                <span>{result.totalWorkingDays ?? "-"}</span>
              </div>
              <div className="p-1.5 px-3 flex gap-2 items-center">
                <span className="font-bold">માંથી હાજર દિવસ</span>
                <span>{result.attendedDays ?? "-"} છે.</span>
              </div>
            </div>

            {/* Marks Table */}
            <table className="w-full text-center text-sm font-semibold border-collapse">
              <thead>
                <tr className="bg-amber-100/90 text-slate-900">
                  <th className="border-b-2 border-r-2 border-slate-900 p-2 w-12 font-bold">
                    ક્રમ
                  </th>
                  <th className="border-b-2 border-r-2 border-slate-900 p-2 font-bold text-center">
                    વિષય
                  </th>
                  <th className="border-b-2 border-r-2 border-slate-900 p-2 w-24 font-bold">
                    કુલ ગુણ
                  </th>
                  <th className="border-b-2 border-r-2 border-slate-900 p-2 w-28 font-bold">
                    મેળવેલ ગુણ
                  </th>
                  <th className="border-b-2 border-r-2 border-slate-900 p-2 w-20 font-bold">
                    ગ્રેડ
                  </th>
                  <th className="border-b-2 border-slate-900 p-2 font-bold">
                    વિષયના સંદર્ભમાં નોંધ
                  </th>
                </tr>
              </thead>
              <tbody>
                {result.subjects.map((sub, idx) => (
                  <tr key={sub.id}>
                    <td className="border-b border-r-2 border-slate-900 p-2">{idx + 1}</td>
                    <td className="border-b border-r-2 border-slate-900 p-2 text-left pl-4 font-semibold">
                      {sub.subjectName}
                    </td>
                    <td className="border-b border-r-2 border-slate-900 p-2 font-semibold">
                      {sub.maximumMarks}
                    </td>
                    <td className="border-b border-r-2 border-slate-900 p-2 font-semibold">
                      {sub.obtainedMarks ?? "-"}
                    </td>
                    <td className="border-b border-r-2 border-slate-900 p-2 font-bold">
                      {sub.grade ?? "-"}
                    </td>
                    <td className="border-b border-slate-900 p-2"></td>
                  </tr>
                ))}
                <tr className="bg-amber-100/90 border-t-2 border-b-2 border-slate-900 text-slate-900">
                  <td
                    colSpan={2}
                    className="border-r-2 border-slate-900 p-2.5 text-center font-extrabold text-base"
                  >
                    મેળવેલ કુલ ગુણ / ગ્રેડ
                  </td>
                  <td className="border-r-2 border-slate-900 p-2.5 font-bold text-base">
                    {result.totalMarks ?? "-"}
                  </td>
                  <td className="border-r-2 border-slate-900 p-2.5 font-bold text-base">
                    {result.obtainedMarks ?? "-"}
                  </td>
                  <td className="border-r-2 border-slate-900 p-2.5 font-extrabold text-base">
                    {result.overallGrade ?? "-"}
                  </td>
                  <td className="p-2.5 text-center font-bold text-base">
                    ટકા : &nbsp;&nbsp;&nbsp;{result.percentage ?? "-"} %
                  </td>
                </tr>
              </tbody>
            </table>

            {/* Footer Date */}
            <div className="grid grid-cols-[auto_1fr] border-b-2 border-slate-900 text-sm font-bold">
              <div className="p-2 px-4 border-r-2 border-slate-900">પરિણામ તારીખ :</div>
              <div className="p-2 px-4">{formatDate(settings?.resultDate || "08/06/2026")}</div>
            </div>

            {/* Signatures */}
            <div className="h-28 border-b-2 border-slate-900 relative bg-white">
              {settings?.classTeacherSignatureUrl && (
                <img
                  src={settings.classTeacherSignatureUrl}
                  alt="Class teacher signature"
                  className="absolute bottom-8 left-12 h-12 max-w-40 object-contain"
                />
              )}
              {settings?.principalSignatureUrl && (
                <img
                  src={settings.principalSignatureUrl}
                  alt="Principal signature"
                  className="absolute bottom-8 right-12 h-12 max-w-40 object-contain"
                />
              )}
              <div className="absolute bottom-2 left-12 font-bold text-sm text-slate-900">
                વર્ગ શિક્ષકની સહી
              </div>
              <div className="absolute bottom-2 right-12 font-bold text-sm text-slate-900">
                આચાર્યની સહી
              </div>
            </div>

            {/* Note Lines */}
            <div className="bg-cyan-50/70 p-3 text-sm font-medium space-y-2 border-t border-slate-300">
              {settings?.footerLineOne ? (
                <p className="text-slate-900">{settings.footerLineOne}</p>
              ) : (
                <p className="text-slate-900">
                  ઉનાળું વેકેશન પૂરું થતાં તારીખ ૦૮/૦૬/૨૦૨૬ ને સોમવારના રોજ સવારે ૬ : ૫૦ કલાક થી
                  શાળા રાબેતા મુજબ શરુ થશે.
                </p>
              )}
              {settings?.footerLineTwo ? (
                <p className="text-center text-xs mt-2 text-slate-700">{settings.footerLineTwo}</p>
              ) : (
                <p className="text-center text-xs mt-2 text-slate-700">
                  80 કે તેથી વધુ A ગ્રેડ, 65 કે તેથી વધુ B ગ્રેડ, 50 કે તેથી વધુ C ગ્રેડ, 35 કે તેથી
                  વધુ D ગ્રેડ, 35 થી ઓછા E ગ્રેડ.
                </p>
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
