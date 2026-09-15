const ERROR_MESSAGES_GU: Record<string, string> = {
  authentication_failed: "ઈમેઈલ અથવા પાસવર્ડ ખોટો છે.",
  csrf_invalid: "સુરક્ષા ટોકન અમાન્ય છે. કૃપા કરીને પાનું ફરી લોડ કરો.",
  login_rate_limited: "ઘણા નિષ્ફળ પ્રયાસો થયા છે. કૃપા કરીને થોડી મિનિટો પછી પ્રયત્ન કરો.",
  school_not_configured: "શાળાની વિગતો હજી ગોઠવવામાં આવી નથી.",
  academic_year_not_found: "ચાલુ શૈક્ષણિક વર્ષની માહિતી ઉપલબ્ધ નથી.",
  resource_not_found: "વિનંતી કરેલી માહિતી મળી શકી નથી.",
  validation_failed: "દાખલ કરેલી વિગતોમાં ભૂલ છે. કૃપા કરીને ફરી તપાસો.",
  password_too_weak: "પાસવર્ડ ઓછામાં ઓછો ૧૨ અક્ષરનો હોવો જોઈએ.",
  password_matches_current: "નવો પાસવર્ડ જૂના પાસવર્ડ જેવો જ ન હોવો જોઈએ.",
  unauthorized: "આ વિભાગ માટે અધિકાર નથી.",
  network_error: "નેટવર્ક કનેક્શન તપાસો અને ફરી પ્રયત્ન કરો.",
};

export function getGujaratiErrorMessage(code?: string, fallback = "હમણાં વિનંતી પૂર્ણ થઈ શકી નથી. કૃપા કરીને ફરી પ્રયત્ન કરો."): string {
  if (!code) {
    return fallback;
  }
  return ERROR_MESSAGES_GU[code] ?? fallback;
}
