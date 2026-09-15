import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { PrincipalProfileResponse, SchoolResponse } from "@/api/types";
import { DEFAULT_SCHOOL_INFO } from "@/utils/gujarati";

export function usePublicSchool() {
  return useQuery({
    queryKey: queryKeys.school,
    queryFn: () => apiRequest<SchoolResponse>("/api/v1/public/school"),
  });
}

export function usePublicPrincipalProfile() {
  return useQuery({
    queryKey: queryKeys.principalProfile,
    queryFn: () => apiRequest<PrincipalProfileResponse>("/api/v1/public/principal-profile"),
  });
}

export function useEffectiveSchoolInfo() {
  const { data: school, isLoading, error } = usePublicSchool();

  return {
    isLoading,
    error,
    name: school?.name || DEFAULT_SCHOOL_INFO.name,
    shortName: school?.shortName || DEFAULT_SCHOOL_INFO.shortName,
    schoolCode: school?.schoolCode || DEFAULT_SCHOOL_INFO.diseCode,
    address: school?.address || DEFAULT_SCHOOL_INFO.address,
    city: school?.city || DEFAULT_SCHOOL_INFO.city,
    state: school?.state || "ગુજરાત",
    postalCode: school?.postalCode || "",
    email: school?.email || DEFAULT_SCHOOL_INFO.email,
    phone: school?.phone || "",
    about: school?.about || "",
    establishedYear: school?.establishedYear ?? DEFAULT_SCHOOL_INFO.establishedYear,
  };
}
