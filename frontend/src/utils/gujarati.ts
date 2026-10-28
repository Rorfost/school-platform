export const DEFAULT_SCHOOL_INFO = {
    name: "",
    shortName: "",
    address: "",
    city: "",
    taluka: "",
    district: "",
    establishedYear: undefined as number | string | undefined,
    establishedDate: "",
    diseCode: "",
    email: "",
    phone: "",
    motto: "",
};

const GUJARATI_DIGITS = ["૦", "૧", "૨", "૩", "૪", "૫", "૬", "૭", "૮", "૯"];

/**
 * Converts Western digits (0-9) to standard Gujarati numeral glyphs (૦-૯).
 * Retains whitespace, hyphens, and non-digit characters without distortion.
 */
export function toGujaratiNumber(input: number | string | null | undefined): string {
    if (input === null || input === undefined) {
        return "";
    }
    return String(input).replace(/[0-9]/g, (digit) => GUJARATI_DIGITS[Number(digit)] ?? digit);
}

/**
 * Standard Gujarati UI label dictionary for public visitor/student views.
 * Admin views retain English labels.
 */
export const LABELS = {
    // Navigation & sections
    home: "હોમ",
    about: "શાળા વિશે",
    studentCorner: "વિદ્યાર્થી વિભાગ",
    materials: "અભ્યાસ સામગ્રી",
    results: "પરીક્ષા પરિણામ",
    tools: "શાળા ઉપયોગી સાધનો",
    downloads: "ડાઉનલોડ",
    notices: "સૂચનાઓ",
    gallery: "ફોટો ગેલેરી",
    contact: "સંપર્ક",
    principalDesk: "આચાર્યશ્રીનો સંદેશ",

    // Assessment & marksheet naming
    annualExam: "વાર્ષિક પરીક્ષા",
    trimasikKasoti: "ત્રિમાસિક કસોટી",
    annualExamResult: "વાર્ષિક પરીક્ષા પરિણામ",
    trimasikKasotiResult: "ત્રિમાસિક કસોટી પરિણામ",
    examResult: "પરીક્ષા પરિણામ",
    selectStandard: "ધોરણ પસંદ કરો",
    viewResult: "પરિણામ જુઓ",
    searchResult: "પરિણામ શોધો",
    studentName: "વિદ્યાર્થીનું નામ",
    standard: "ધોરણ",
    classLabel: "વર્ગ",
    rollNumber: "રોલ નંબર",
    generalRegisterNumber: "જ. રજી. નંબર",
    birthDate: "જન્મ તારીખ",
    totalMarks: "કુલ ગુણ",
    obtainedMarks: "મેળવેલ ગુણ",
    percentage: "ટકાવારી",
    rank: "ક્રમ",
    grade: "ગ્રેડ",
    resultStatus: "પરિણામ",
    remarks: "વિષયના સંદર્ભમાં નોંધ",

    // Feedback & state messages
    loading: "લોડ થઈ રહ્યું છે...",
    tryAgain: "ફરી પ્રયત્ન કરો",
    empty: "હાલ કોઈ માહિતી ઉપલબ્ધ નથી.",
    errorFallback: "હમણાં માહિતી મળી શકી નથી. ફરી પ્રયત્ન કરો.",
    notFound: "માહિતી મળી નથી.",
    noNotices: "હાલ કોઈ સૂચના ઉપલબ્ધ નથી.",
    noMaterials: "હાલ કોઈ અભ્યાસ સામગ્રી ઉપલબ્ધ નથી.",
    noDownloads: "હાલ કોઈ ડાઉનલોડ ફાઇલ ઉપલબ્ધ નથી.",
    noAlbums: "હાલ કોઈ ફોટો આલ્બમ ઉપલબ્ધ નથી.",

    // School meta & identity
    diseLabel: "ડાયસ કોડ",
    estLabel: "સ્થાપના",
    adminLogin: "વ્યવસ્થાપન પ્રવેશ",
    back: "પાછા જાઓ",
    readMore: "વધુ વાંચો",
    downloadFile: "ડાઉનલોડ કરો",
    printReport: "પ્રિન્ટ પરિણામ",
} as const;

export type LabelKey = keyof typeof LABELS;
