import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "../../api/client";
import { queryKeys } from "../../api/queryKeys";
import { AcademicYearResponse, StandardResponse, SubjectResponse } from "../../api/types";

export interface AssessmentTypeResponse {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  sortOrder: number;
}

export function usePublicStandards() {
  return useQuery<StandardResponse[]>({
    queryKey: queryKeys.standards,
    queryFn: () => apiRequest<StandardResponse[]>("/api/v1/public/standards"),
  });
}

export function usePublicSubjects() {
  return useQuery<SubjectResponse[]>({
    queryKey: queryKeys.subjects,
    queryFn: () => apiRequest<SubjectResponse[]>("/api/v1/public/subjects"),
  });
}

export function usePublicCurrentYear() {
  return useQuery<AcademicYearResponse>({
    queryKey: queryKeys.currentAcademicYear,
    queryFn: () => apiRequest<AcademicYearResponse>("/api/v1/public/academic-years/current"),
  });
}

export function usePublicAssessmentTypes() {
  return useQuery<AssessmentTypeResponse[]>({
    queryKey: queryKeys.assessmentTypes,
    queryFn: () => apiRequest<AssessmentTypeResponse[]>("/api/v1/public/assessment-types"),
  });
}
