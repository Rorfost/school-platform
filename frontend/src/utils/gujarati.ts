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

export function toGujaratiNumber(input: number | string | null | undefined): string {
    if (input === null || input === undefined) {
        return "";
    }
    return String(input).replace(/[0-9]/g, (digit) => GUJARATI_DIGITS[Number(digit)] ?? digit);
}

export const LABELS = {
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
    selectStandard: "ધોરણ પસંદ કરો",
    viewResult: "પરિણામ જુઓ",
    loading: "લોડ થઈ રહ્યું છે...",
    tryAgain: "ફરી પ્રયત્ન કરો",
    empty: "હાલ કોઈ માહિતી ઉપલબ્ધ નથી.",
    errorFallback: "હમણાં માહિતી મળી શકી નથી. ફરી પ્રયત્ન કરો.",
    diseLabel: "ડાયસ કોડ",
    estLabel: "સ્થાપના",
    adminLogin: "વ્યવસ્થાપન પ્રવેશ",
    back: "પાછા જાઓ",
} as const;
