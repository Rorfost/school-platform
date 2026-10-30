import { Printer } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import { Button } from "@/components/ui/Button";
import type { ExamResultResponse, ResultPresentationSettingsResponse } from "@/api/types";
import { formatDate } from "@/utils/date";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

export function ExamResultViewer({
    result,
    schoolName,
    logoUrl,
    sealUrl,
    resultType = "ANNUAL",
}: {
    result: ExamResultResponse;
    schoolName: string;
    logoUrl?: string | null;
    sealUrl?: string | null;
    resultType?: "ANNUAL" | "EKAM_KASOTI";
}) {
    const { data: settings } = useQuery<ResultPresentationSettingsResponse>({
        queryKey: queryKeys.resultSettings(result.standard, resultType),
        queryFn: () =>
            apiRequest<ResultPresentationSettingsResponse>(
                `/api/v1/public/result-settings?standard=${encodeURIComponent(result.standard)}`,
            ),
    });
    const schoolInfo = useEffectiveSchoolInfo();
    const effectiveSealUrl = sealUrl || schoolInfo.sealUrl;
    const locationSubtitle = [
        schoolInfo.taluka ? `તા. ${schoolInfo.taluka}` : "",
        schoolInfo.district ? `જિ. ${schoolInfo.district}` : "",
        schoolInfo.schoolCode ? `ડાયસ કોડ: ${schoolInfo.schoolCode}` : "",
    ]
        .filter(Boolean)
        .join(", ");

    const handlePrint = () => window.print();

    const isEkam = resultType === "EKAM_KASOTI";
    //   const resultTypeLabel = isEkam ? "ત્રિમાસિક કસોટી" : "પરીક્ષા";
    const sheetTitle = isEkam ? settings?.ekamResultSheetTitle : settings?.resultSheetTitle;
    const resultDateValue = isEkam ? settings?.ekamResultDate : settings?.resultDate;
    const resultPlaceValue = isEkam ? settings?.ekamResultPlace : settings?.resultPlace;

    return (
        <div className="space-y-4">
            <div className="flex justify-end print:hidden">
                <Button onClick={handlePrint} variant="primary" className="gap-2">
                    <Printer size={16} /> Print Result
                </Button>
            </div>

            <div
                id="exam-result-print"
                className="exam-result-sheet mx-auto bg-white p-1 sm:p-4 text-slate-900 print:p-0 print:m-0 print:w-full print:shadow-none font-sans"
                style={{ maxWidth: "190mm" }}
            >
                <div className="border-2 sm:border-4 border-slate-900 p-0.5 sm:p-1 print:border-4 print:p-1">
                    <div className="border border-slate-900 sm:border-2 print:border-2">
                        {/* Header: Free logo with subtle vertical divider line */}
                        <div className="flex items-center border-b-2 border-slate-900 py-1.5 sm:py-2 px-1 sm:px-3 relative">
                            {logoUrl || schoolInfo.logoUrl ? (
                                <>
                                    <div className="shrink-0 p-1 sm:p-2 flex justify-center items-center">
                                        <img
                                            src={logoUrl || schoolInfo.logoUrl || ""}
                                            alt="School Logo"
                                            className="size-14 sm:size-20 object-contain print:size-20"
                                            crossOrigin="anonymous"
                                        />
                                    </div>
                                    {/* Subtle vertical divider */}
                                    <div
                                        className="h-12 sm:h-16 w-px bg-slate-900/60 mx-1 sm:mx-3 shrink-0"
                                        aria-hidden="true"
                                    />
                                </>
                            ) : null}

                            <div className="flex-1 text-center py-1 px-1 flex flex-col justify-center leading-tight min-w-0">
                                <h1 className="text-sm sm:text-2xl font-bold text-blue-900 tracking-wide print:text-2xl truncate sm:whitespace-normal">
                                    {schoolName || schoolInfo.name}
                                </h1>
                                {locationSubtitle && (
                                    <p className="text-[11px] sm:text-base font-bold text-blue-900 mt-0.5 sm:mt-1 print:text-base truncate sm:whitespace-normal">
                                        {locationSubtitle}
                                    </p>
                                )}
                                {/* Red title line: always single line, width matches text */}
                                <div
                                    className="mt-1 sm:mt-2 text-red-700 font-bold border-t-2 border-red-700 mx-auto w-fit px-3 pt-1"
                                    style={{ fontSize: "clamp(9px, 2.2vw, 16px)" }}
                                >
                                    {sheetTitle}
                                </div>
                            </div>
                        </div>

                        {/* Student Info */}
                        <div className="border-b-2 border-slate-900 p-2 sm:p-3 space-y-1.5 sm:space-y-2 text-xs sm:text-sm font-semibold print:text-sm print:p-2.5 print:space-y-1.5">
                            {/* Line 1: Name, Standard, Class */}
                            <div className="flex flex-wrap sm:flex-nowrap items-center gap-y-1 text-xs sm:text-sm">
                                <div className="w-full sm:w-[52%] shrink-0 flex gap-1.5 items-center min-w-0 pr-1">
                                    <span className="font-bold shrink-0 w-[88px] sm:w-[100px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">વિદ્યાર્થીનું નામ</span>
                                        <span>:</span>
                                    </span>
                                    <span className="truncate">{result.studentName || "-"}</span>
                                </div>
                                <div className="w-[58%] sm:w-[29%] shrink-0 flex gap-1.5 items-center min-w-0 pr-1">
                                    <span className="font-bold shrink-0 w-[88px] sm:w-[86px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">ધોરણ</span>
                                        <span>:</span>
                                    </span>
                                    <span>{result.standard}</span>
                                </div>
                                <div className="w-[42%] sm:w-[29%] shrink-0 flex gap-1.5 items-center min-w-0">
                                    <span className="font-bold shrink-0 w-[68px] sm:w-[76px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">વર્ગ</span>
                                        <span>:</span>
                                    </span>
                                    <span>{result.studentClass || "-"}</span>
                                </div>
                            </div>

                            {/* Line 2: GR No, Birthdate, Roll No */}
                            <div className="flex flex-wrap sm:flex-nowrap items-center gap-y-1 text-xs sm:text-sm">
                                <div className="w-full sm:w-[52%] shrink-0 flex gap-1.5 items-center min-w-0 pr-1">
                                    <span className="font-bold shrink-0 w-[88px] sm:w-[100px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">જ. રજી. નંબર</span>
                                        <span>:</span>
                                    </span>
                                    <span className="truncate">
                                        {result.generalRegisterNumber || "-"}
                                    </span>
                                </div>
                                <div className="w-[58%] sm:w-[29%] shrink-0 flex gap-1.5 items-center min-w-0 pr-1">
                                    <span className="font-bold shrink-0 w-[88px] sm:w-[86px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">જન્મ તારીખ</span>
                                        <span>:</span>
                                    </span>
                                    <span className="shrink-0 whitespace-nowrap">
                                        {formatDate(result.birthDate)}
                                    </span>
                                </div>
                                <div className="w-[42%] sm:w-[19%] shrink-0 flex gap-1.5 items-center min-w-0">
                                    <span className="font-bold shrink-0 w-[68px] sm:w-[76px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">રોલ નં</span>
                                        <span>:</span>
                                    </span>
                                    <span>{result.rollNumber}</span>
                                </div>
                            </div>

                            {/* Line 3: Total Working Days, Present Days */}
                            <div className="flex flex-wrap sm:flex-nowrap items-center gap-y-1 text-xs sm:text-sm">
                                <div className="w-full sm:w-[52%] shrink-0 flex gap-1.5 items-center min-w-0 pr-1">
                                    <span className="font-bold shrink-0 w-[88px] sm:w-[100px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">કુલ કાર્ય દિવસ</span>
                                        <span>:</span>
                                    </span>
                                    <span>{result.totalWorkingDays ?? "-"}</span>
                                </div>
                                <div className="w-[58%] sm:w-[29%] shrink-0 flex gap-1.5 items-center min-w-0 pr-1">
                                    <span className="font-bold shrink-0 w-[88px] sm:w-[86px] flex justify-between whitespace-nowrap">
                                        <span className="whitespace-nowrap">હાજર દિવસ</span>
                                        <span>:</span>
                                    </span>
                                    <span>{result.attendedDays ?? "-"}</span>
                                </div>
                            </div>
                        </div>

                        {/* Marks Table */}
                        <div className="overflow-x-auto print:overflow-visible">
                            <table className="w-full text-center text-xs sm:text-sm font-semibold border-collapse print:table-fixed print:text-sm">
                                <colgroup>
                                    {isEkam ? (
                                        <>
                                            <col className="w-[7%] sm:w-[7%] print:w-[7%]" />
                                            <col className="w-[38%] sm:w-[30%] print:w-[30%]" />
                                            <col className="w-[14%] sm:w-[14%]" />
                                            <col className="w-[16%] sm:w-[17%] print:w-[17%]" />
                                            <col className="w-[25%] sm:w-[32%] print:w-[32%]" />
                                        </>
                                    ) : (
                                        <>
                                            <col className="w-[7%] sm:w-[7%] print:w-[7%]" />
                                            <col className="w-[37%] sm:w-[30%] print:w-[30%]" />
                                            <col className="w-[11%] sm:w-[11%] print:w-[11%]" />
                                            <col className="w-[11%] sm:w-[12%] print:w-[12%]" />
                                            <col className="w-[10%] sm:w-[10%]" />
                                            <col className="w-[24%] sm:w-[30%] print:w-[30%]" />
                                        </>
                                    )}
                                </colgroup>
                                <thead>
                                    <tr className="bg-amber-100/90 text-slate-900">
                                        <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-7 sm:w-10 font-bold print:p-2 print:w-10 whitespace-nowrap">
                                            ક્રમ
                                        </th>
                                        <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 font-bold text-center print:p-2 whitespace-nowrap">
                                            વિષય
                                        </th>
                                        <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-12 sm:w-16 font-bold print:p-2 print:w-16 whitespace-nowrap">
                                            કુલ ગુણ
                                        </th>
                                        <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-12 sm:w-20 font-bold print:p-2 print:w-20 whitespace-nowrap">
                                            મેળવેલ ગુણ
                                        </th>
                                        {!isEkam && (
                                            <th className="border-b-2 border-r-2 border-slate-900 p-1 sm:p-2 w-10 sm:w-14 font-bold print:p-2 print:w-14 whitespace-nowrap">
                                                ગ્રેડ
                                            </th>
                                        )}
                                        <th className="border-b-2 border-slate-900 p-1 sm:p-2 w-16 sm:w-28 font-bold text-center print:p-2 print:w-28 whitespace-normal sm:whitespace-nowrap leading-tight">
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
                                        <td className="p-1 sm:p-2.5 text-center font-bold text-xs sm:text-base print:text-base print:p-2.5 whitespace-nowrap">
                                            ટકા :{" "}
                                            <span className="hidden sm:inline print:inline">
                                                &nbsp;&nbsp;
                                            </span>
                                            {result.percentage ?? "-"} %
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        {/* Place, Date & Signatures Container */}
                        <div className="pt-2 sm:pt-3 pb-2 px-3 sm:px-8 bg-white">
                            {/* Place & Date on left, Parent Sign on right */}
                            <div className="flex justify-between items-end text-xs sm:text-sm font-semibold">
                                <div className="space-y-1">
                                    <div className="flex gap-1.5 items-center">
                                        <span className="font-bold text-slate-900 shrink-0 w-[42px] sm:w-[48px] flex justify-between">
                                            <span>સ્થળ</span>
                                            <span>:</span>
                                        </span>
                                        <span>{resultPlaceValue || schoolInfo.city || "-"}</span>
                                    </div>
                                    <div className="flex gap-1.5 items-center">
                                        <span className="font-bold text-slate-900 shrink-0 w-[42px] sm:w-[48px] flex justify-between">
                                            <span>તારીખ</span>
                                            <span>:</span>
                                        </span>
                                        <span>{formatDate(resultDateValue)}</span>
                                    </div>
                                </div>

                                {/* Right side: Parent Sign (વાલી ની સહી) - empty space for parents to sign */}
                                <div className="flex flex-col items-center justify-end text-center min-w-[90px] sm:min-w-[120px]">
                                    <div className="h-5 sm:h-6" aria-hidden="true" />
                                    <span className="font-bold text-xs sm:text-sm text-slate-900 whitespace-nowrap border-t border-slate-400 pt-1.5 px-8 sm:px-10">
                                        વાલીની સહી
                                    </span>
                                </div>
                            </div>

                            {/* Signatures & School Seal */}
                            <div className="flex justify-between items-end pt-1 min-h-[100px] sm:min-h-[130px] print:min-h-[130px] gap-1 sm:gap-2">
                                {/* 1. Class Teacher Sign */}
                                <div className="flex flex-col items-center justify-end min-w-0 w-[30%] sm:min-w-[130px] sm:w-auto text-center gap-1 print:min-w-[130px]">
                                    {settings?.classTeacherSignatureUrl ? (
                                        <img
                                            src={settings.classTeacherSignatureUrl}
                                            alt="Class teacher signature"
                                            className="h-9 sm:h-12 w-full max-w-[90px] sm:max-w-40 object-contain print:h-12 print:max-w-40"
                                            crossOrigin="anonymous"
                                        />
                                    ) : (
                                        <div
                                            className="h-9 sm:h-12 print:h-12"
                                            aria-hidden="true"
                                        />
                                    )}
                                    <span className="font-bold text-[10px] sm:text-sm text-slate-900 print:text-sm whitespace-nowrap border-t border-slate-400 pt-1 px-1 sm:px-2 w-full text-center">
                                        વર્ગ શિક્ષકની સહી
                                    </span>
                                </div>

                                {/* 2. School Seal (શાળાનો સિક્કો) in the middle */}
                                <div className="flex flex-col items-center justify-end min-w-0 w-[36%] sm:min-w-[140px] sm:w-auto text-center gap-1 print:min-w-[140px]">
                                    {effectiveSealUrl ? (
                                        <img
                                            src={effectiveSealUrl}
                                            alt="School seal"
                                            className="h-16 w-16 sm:h-28 sm:w-28 object-contain print:h-28 print:w-28 mx-auto"
                                            crossOrigin="anonymous"
                                        />
                                    ) : (
                                        <div
                                            className="h-16 sm:h-28 print:h-28"
                                            aria-hidden="true"
                                        />
                                    )}
                                    <span className="font-bold text-[10px] sm:text-sm text-slate-900 print:text-sm whitespace-nowrap border-t border-slate-400 pt-1 px-1 sm:px-2 w-full text-center">
                                        શાળાનો સિક્કો
                                    </span>
                                </div>

                                {/* 3. Principal Sign */}
                                <div className="flex flex-col items-center justify-end min-w-0 w-[34%] sm:min-w-[160px] sm:w-auto text-center gap-1 print:min-w-[160px]">
                                    {settings?.principalSignatureUrl ? (
                                        <img
                                            src={settings.principalSignatureUrl}
                                            alt="Principal seal and signature"
                                            className="h-16 w-full max-w-[110px] sm:h-27 sm:max-w-64 object-contain print:h-27 print:max-w-64"
                                            crossOrigin="anonymous"
                                        />
                                    ) : (
                                        <div
                                            className="h-16 sm:h-27 print:h-27"
                                            aria-hidden="true"
                                        />
                                    )}
                                    <span className="font-bold text-[10px] sm:text-sm text-slate-900 print:text-sm whitespace-nowrap border-t border-slate-400 pt-1 px-1 sm:px-2 w-full text-center">
                                        આચાર્યની સહી
                                    </span>
                                </div>
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
        @page {
          size: A4 portrait;
          margin: 8mm;
        }
        @media print {
          html, body { margin: 0 !important; padding: 0 !important; background: white !important; }
          body * { visibility: hidden !important; }
          #exam-result-print, #exam-result-print * { visibility: visible !important; }
          #exam-result-print {
            position: absolute !important;
            inset: 0 auto auto 0 !important;
            max-width: none !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 4mm 4mm !important;
            background: white !important;
            box-shadow: border-box !important;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
          }
        }
      `}</style>
        </div>
    );
}
