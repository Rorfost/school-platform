import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { queryKeys } from "@/api/queryKeys";
import type { AuditLogResponse, DashboardSummaryResponse } from "@/api/types";

export function useAdminDashboardSummary() {
  return useQuery({
    queryKey: queryKeys.adminDashboardSummary,
    queryFn: () => apiRequest<DashboardSummaryResponse>("/api/v1/admin/dashboard/summary"),
  });
}

export function useAdminAuditLogs() {
  return useQuery({
    queryKey: queryKeys.adminAuditLogs,
    queryFn: () => apiRequest<AuditLogResponse[]>("/api/v1/admin/audit-logs"),
  });
}
