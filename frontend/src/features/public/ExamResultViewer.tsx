import { Printer } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { Button } from "@/components/ui/Button";
import type { ExamResultResponse, ResultPresentationSettingsResponse } from "@/api/types";
import { toGujaratiNumber } from "@/utils/gujarati";
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
    queryKey: ["result-settings"],
    queryFn: () => apiRequest<ResultPresentationSettingsResponse>("/api/v1/public/result-settings"),
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
        className="mx-auto bg-white p-2 sm:p-4 text-black print:p-0 print:m-0 print:w-full print:shadow-none font-sans"
        style={{ maxWidth: "210mm" }}
      >
        <div className="border-4 border-slate-800 p-1">
          <div className="border-2 border-slate-800">
            {/* Header */}
            <div className="flex items-center border-b-2 border-slate-800">
              <div className="w-[120px] shrink-0 p-2 border-r-2 border-slate-800 flex justify-center items-center">
                <img
                  src={logoUrl || schoolLogo}
                  alt="School Logo"
                  className="size-20 object-contain"
                  crossOrigin="anonymous"
                />
              </div>
              <div className="flex-1 text-center py-2 flex flex-col justify-center">
                <h1 className="text-xl sm:text-2xl font-bold text-blue-800 tracking-wide">
                  {schoolName}
                </h1>
                <p className="text-sm sm:text-base font-bold text-blue-800 mt-1">
                  તા. સમી, જિ. પાટણ
                </p>
                <div className="mt-2 text-red-700 font-bold border-t-2 border-red-700 mx-auto w-3/4 pt-1">
                  પરિણામ પત્રક : ૨૦૨૫-૨૬
                </div>
              </div>
            </div>

            {/* Student Info */}
            <div className="grid grid-cols-[1fr_auto] sm:grid-cols-[1fr_80px_100px] border-b-2 border-slate-800 text-sm">
              <div className="border-r-2 border-slate-800 p-1.5 px-3 flex gap-2">
                <span className="font-bold text-blue-900">વિદ્યાર્થીનું નામ :</span>
                <span className="font-bold">{result.studentName}</span>
              </div>
              <div className="border-r-2 border-slate-800 p-1.5 px-3 flex gap-2 sm:col-span-1 col-span-2 border-t-2 sm:border-t-0">
                <span className="font-bold">ધોરણ :</span>
                <span>{toGujaratiNumber(result.standard)}</span>
              </div>
              <div className="p-1.5 px-3 flex gap-2 hidden sm:flex">
                <span className="font-bold">વર્ગ :</span>
                <span>-</span>
              </div>
            </div>

            <div className="grid grid-cols-[auto_1fr_auto_1fr] sm:grid-cols-[auto_1fr_auto_1fr_80px_100px] border-b-2 border-slate-800 text-sm">
              <div className="p-1.5 px-3 font-bold border-r border-slate-800">જનરલ રજી.નંબર :</div>
              <div className="p-1.5 px-3 border-r-2 border-slate-800">
                {result.generalRegisterNumber || "-"}
              </div>
              <div className="p-1.5 px-3 font-bold border-r border-slate-800">જન્મ તારીખ :</div>
              <div className="p-1.5 px-3 border-r-2 border-slate-800 sm:col-span-1 col-span-3 border-b-2 sm:border-b-0">
                {result.birthDate || "-"}
              </div>
              <div className="p-1.5 px-3 font-bold border-r border-slate-800 hidden sm:block">
                રોલ નં :
              </div>
              <div className="p-1.5 px-3 hidden sm:block">{result.rollNumber}</div>
            </div>

            <div className="grid grid-cols-[auto_1fr_auto_1fr_auto_1fr] border-b-2 border-slate-800 text-sm">
              <div className="p-1.5 px-3 font-bold border-r border-slate-800">કુલ કાર્ય દિવસ :</div>
              <div className="p-1.5 px-3 border-r-2 border-slate-800">
                {result.totalWorkingDays ?? "-"}
              </div>
              <div className="p-1.5 px-3 font-bold border-r border-slate-800">માંથી હાજર દિવસ</div>
              <div className="p-1.5 px-3 border-r-2 border-slate-800">
                {result.attendedDays ?? "-"} છે.
              </div>
              <div className="p-1.5 px-3 font-bold border-r border-slate-800 sm:hidden block">
                રોલ નં :
              </div>
              <div className="p-1.5 px-3 sm:hidden block">{result.rollNumber}</div>
            </div>

            {/* Marks Table */}
            <table className="w-full text-center text-sm font-semibold border-collapse">
              <thead>
                <tr className="bg-yellow-100">
                  <th className="border-b-2 border-r-2 border-slate-800 p-2 w-12">ક્રમ</th>
                  <th className="border-b-2 border-r-2 border-slate-800 p-2">વિષય</th>
                  <th className="border-b-2 border-r-2 border-slate-800 p-2 w-20">કુલ ગુણ</th>
                  <th className="border-b-2 border-r-2 border-slate-800 p-2 w-20">મેળવેલ ગુણ</th>
                  <th className="border-b-2 border-r-2 border-slate-800 p-2 w-16">ગ્રેડ</th>
                  <th className="border-b-2 border-slate-800 p-2">વિષયના સંદર્ભમાં નોંધ</th>
                </tr>
              </thead>
              <tbody>
                {result.subjects.map((sub, idx) => (
                  <tr key={sub.id}>
                    <td className="border-b border-r-2 border-slate-800 p-1.5">{idx + 1}</td>
                    <td className="border-b border-r-2 border-slate-800 p-1.5 text-left pl-3">
                      {sub.subjectName}
                    </td>
                    <td className="border-b border-r-2 border-slate-800 p-1.5">
                      {sub.maximumMarks}
                    </td>
                    <td className="border-b border-r-2 border-slate-800 p-1.5">
                      {sub.obtainedMarks ?? "-"}
                    </td>
                    <td className="border-b border-r-2 border-slate-800 p-1.5">
                      {sub.grade ?? "-"}
                    </td>
                    <td className="border-b border-slate-800 p-1.5"></td>
                  </tr>
                ))}
                <tr className="bg-yellow-100 border-y-2 border-slate-800 text-blue-900">
                  <td
                    colSpan={2}
                    className="border-r-2 border-slate-800 p-2 text-right pr-4 font-bold"
                  >
                    મેળવેલ કુલ ગુણ / ગ્રેડ
                  </td>
                  <td className="border-r-2 border-slate-800 p-2 font-bold">
                    {result.totalMarks ?? "-"}
                  </td>
                  <td className="border-r-2 border-slate-800 p-2 font-bold">
                    {result.obtainedMarks ?? "-"}
                  </td>
                  <td className="border-r-2 border-slate-800 p-2 font-bold">
                    {result.overallGrade ?? "-"}
                  </td>
                  <td className="p-2 text-left pl-3 font-bold">
                    ટકા : &nbsp;&nbsp;&nbsp;{result.percentage ?? "-"} %
                  </td>
                </tr>
              </tbody>
            </table>

            {/* Footer */}
            <div className="grid grid-cols-[auto_1fr] border-b-2 border-slate-800 text-sm">
              <div className="p-2 px-4 font-bold border-r border-slate-800">પરિણામ તારીખ :</div>
              <div className="p-2 px-4">{settings?.resultDate ?? "-"}</div>
            </div>

            <div className="h-24 border-b-2 border-slate-800 relative">
              <div className="absolute bottom-2 left-10 font-bold text-sm">વર્ગ શિક્ષકની સહી</div>
              <div className="absolute bottom-2 right-10 font-bold text-sm">આચાર્યની સહી</div>
            </div>

            <div className="bg-cyan-50 p-3 text-sm font-medium space-y-2">
              <p>
                ઉનાળું વેકેશન પૂરું થતાં તારીખ ૦૮/૦૬/૨૦૨૬ ને સોમવારના રોજ સવારે ૬ : ૫૦ કલાક થી શાળા
                રાબેતા મુજબ શરુ થશે.
              </p>
              <p className="text-center text-xs mt-2 text-slate-700">
                80 કે તેથી વધુ A ગ્રેડ, 65 કે તેથી વધુ B ગ્રેડ, 50 કે તેથી વધુ C ગ્રેડ, 35 કે તેથી
                વધુ D ગ્રેડ, 35 થી ઓછા E ગ્રેડ.
              </p>
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
          }
        }
      `}</style>
    </div>
  );
}
