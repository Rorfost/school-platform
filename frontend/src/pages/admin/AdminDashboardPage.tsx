import {
  Activity,
  Award,
  Calendar,
  Camera,
  FileText,
  FolderDown,
  GraduationCap,
  Layers,
  Settings,
  UserCheck,
  Users,
  RotateCcw,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/api/client";
import { ErrorState } from "@/components/common/StatusPanel";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import type { VisitResponse } from "@/api/types";
import { useAdminAuditLogs, useAdminDashboardSummary } from "@/features/admin/useAdminData";
import { useAuth } from "@/features/auth/useAuth";
import { useEffectiveSchoolInfo, usePublicPrincipalProfile } from "@/features/school/useSchoolData";

export function AdminDashboardPage() {
  const { principal } = useAuth();
  const school = useEffectiveSchoolInfo();
  const { data: profile } = usePublicPrincipalProfile();
  const {
    data: summary,
    isLoading: isSummaryLoading,
    isError: isSummaryError,
    refetch: refetchSummary,
  } = useAdminDashboardSummary();
  const { data: auditLogs } = useAdminAuditLogs();
  const visitsQuery = useQuery({
    queryKey: ["public", "visits"],
    queryFn: () => apiRequest<VisitResponse>("/api/v1/public/visits"),
  });
  const resetVisitsMutation = useMutation({
    mutationFn: () =>
      apiRequest<VisitResponse>("/api/v1/admin/site-metrics/visits/reset", { method: "POST" }),
    onSuccess: () => visitsQuery.refetch(),
  });

  const quickLinks = [
    {
      to: "/admin/school",
      label: "School Identity",
      icon: Settings,
      desc: "Name, address, contact, DISE code",
    },
    {
      to: "/admin/principal",
      label: "Principal Profile",
      icon: UserCheck,
      desc: "Biography, message, designation",
    },
    {
      to: "/admin/academics",
      label: "Academic Setup",
      icon: Calendar,
      desc: "Current year, standards, and subjects",
    },
    {
      to: "/admin/assessments",
      label: "Results & Marks",
      icon: Award,
      desc: "Annual and Ekam Kasoti result uploads",
    },
    {
      to: "/admin/materials",
      label: "Study Materials",
      icon: GraduationCap,
      desc: "Worksheets & PDF files",
    },
    {
      to: "/admin/notices",
      label: "Notices & Circulars",
      icon: FileText,
      desc: "School announcements",
    },
    {
      to: "/admin/gallery",
      label: "Photo Gallery",
      icon: Camera,
      desc: "Event albums & photos",
    },
    {
      to: "/admin/downloads",
      label: "Downloads",
      icon: FolderDown,
      desc: "Public forms & documents",
    },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="rounded-2xl bg-gradient-to-r from-blue-900 to-slate-900 p-6 sm:p-8 text-white shadow-md">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs uppercase font-bold tracking-widest text-blue-200">
              Administrative Control Panel
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold mt-1">
              Welcome back, {profile?.fullName || principal?.email || "Principal"}
            </h1>
            <p className="text-sm text-slate-300 mt-1">
              {school.name} — DISE Code: {school.schoolCode || "24030401801"}
            </p>
          </div>
          <Link
            to="/admin/account"
            className="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-xs sm:text-sm font-semibold transition-colors shrink-0 border border-white/10"
          >
            Manage Account & Password
          </Link>
        </div>
      </div>

      {/* Real Summary Stats Grid */}
      {isSummaryLoading ? (
        <DashboardSummarySkeleton />
      ) : isSummaryError ? (
        <ErrorState message="Could not load dashboard summary." onRetry={() => refetchSummary()} />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase">Active Students</span>
              <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                <Users size={18} aria-hidden="true" />
              </div>
            </div>
            <p className="text-2xl font-extrabold text-slate-900 mt-2">
              {summary?.totalStudents ?? 0}
            </p>
            <span className="text-xs text-slate-500 mt-1 block">Enrolled across standards</span>
          </Card>

          <Card className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase">Standards</span>
              <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                <Layers size={18} aria-hidden="true" />
              </div>
            </div>
            <p className="text-2xl font-extrabold text-slate-900 mt-2">
              {summary?.totalStandards ?? 0}
            </p>
            <Link
              to="/admin/academics"
              className="text-xs font-semibold text-blue-900 hover:underline mt-1 block"
            >
              Manage academic setup →
            </Link>
          </Card>

          <Card className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase">
                Published Materials
              </span>
              <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                <GraduationCap size={18} aria-hidden="true" />
              </div>
            </div>
            <p className="text-2xl font-extrabold text-slate-900 mt-2">
              {summary?.publishedMaterials ?? 0}
            </p>
            <Link
              to="/admin/materials"
              className="text-xs font-semibold text-blue-900 hover:underline mt-1 block"
            >
              Manage materials →
            </Link>
          </Card>

          <Card className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase">Published Notices</span>
              <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
                <FileText size={18} aria-hidden="true" />
              </div>
            </div>
            <p className="text-2xl font-extrabold text-slate-900 mt-2">
              {summary?.publishedNotices ?? 0}
            </p>
            <Link
              to="/admin/notices"
              className="text-xs font-semibold text-blue-900 hover:underline mt-1 block"
            >
              Manage notices →
            </Link>
          </Card>
        </div>
      )}

      <Card className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-base font-bold text-slate-900">Website visits</h2>
          <p className="mt-1 text-sm text-slate-600">
            Current counter:{" "}
            <span className="font-semibold text-slate-900">
              {visitsQuery.data?.totalVisits ?? 0}
            </span>
          </p>
        </div>
        <Button
          variant="outline"
          className="gap-2"
          loading={resetVisitsMutation.isPending}
          onClick={() => {
            if (window.confirm("Reset the website visit counter to zero?")) {
              resetVisitsMutation.mutate();
            }
          }}
        >
          <RotateCcw size={16} aria-hidden="true" /> Reset visit counter
        </Button>
      </Card>

      {/* Operational Shortcuts */}
      <div>
        <h2 className="text-lg font-bold text-slate-900 mb-4">Management Modules</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {quickLinks.map((item) => {
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                className="group p-5 rounded-xl border border-slate-200 bg-white shadow-xs hover:border-blue-300 hover:shadow-md transition-all flex items-start gap-4"
              >
                <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700 group-hover:bg-blue-900 group-hover:text-white transition-colors">
                  <Icon size={20} aria-hidden="true" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 group-hover:text-blue-900 transition-colors">
                    {item.label}
                  </h3>
                  <p className="text-xs text-slate-600 mt-0.5 leading-relaxed">{item.desc}</p>
                </div>
              </Link>
            );
          })}
        </div>
      </div>

      {/* Recent Security & Audit Logs */}
      {auditLogs && auditLogs.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-xs">
          <div className="flex items-center gap-2 mb-4">
            <Activity size={18} className="text-blue-900" aria-hidden="true" />
            <h2 className="text-base font-bold text-slate-900">Recent Security Activity</h2>
          </div>
          <div className="divide-y divide-slate-100">
            {auditLogs.slice(0, 5).map((log) => (
              <div
                key={log.id}
                className="py-3 flex items-center justify-between text-xs sm:text-sm"
              >
                <div>
                  <span className="font-semibold text-slate-900 uppercase">{log.action}</span>
                  <span className="text-slate-500 ml-2 font-mono text-xs">
                    Target: {log.targetType}
                  </span>
                </div>
                <span className="text-slate-400 text-xs">
                  {new Date(log.createdAt).toLocaleString()}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function DashboardSummarySkeleton() {
  return (
    <div
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
      aria-busy="true"
      aria-label="Loading dashboard summary"
    >
      {Array.from({ length: 4 }, (_, index) => (
        <Card key={index} className="space-y-3 p-5">
          <div className="h-3 w-2/3 rounded bg-slate-200" />
          <div className="h-8 w-1/3 rounded bg-slate-100" />
          <div className="h-3 w-4/5 rounded bg-slate-100" />
        </Card>
      ))}
    </div>
  );
}
