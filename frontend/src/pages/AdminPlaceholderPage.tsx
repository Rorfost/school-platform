import { AlertTriangle, CheckCircle2, Shield, User } from "lucide-react";
import { Card, CardHeader } from "@/components/ui/Card";
import { useAuth } from "@/features/auth/AuthContext";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

export function AdminPlaceholderPage() {
  const { principal } = useAuth();
  const school = useEffectiveSchoolInfo();

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Principal Administration
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Secure administration workspace for {school.name}.
        </p>
      </div>

      {principal?.mustChangePassword && (
        <div
          className="flex items-start gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950 shadow-xs"
          role="alert"
        >
          <AlertTriangle className="mt-0.5 shrink-0 text-amber-600" size={18} aria-hidden="true" />
          <div>
            <p className="font-semibold">Password update required</p>
            <p className="mt-0.5 text-amber-800">
              Your account has a default or temporary password. Please update your password to maintain account security.
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
        <Card>
          <CardHeader
            title="Principal Account"
            subtitle="Current authenticated administrator"
            action={<User className="text-slate-400" size={20} />}
          />
          <dl className="space-y-2.5 text-sm">
            <div>
              <dt className="text-xs text-slate-500 font-medium">Email</dt>
              <dd className="font-semibold text-slate-800">{principal?.email}</dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500 font-medium">Role</dt>
              <dd className="font-medium text-slate-800">{principal?.role}</dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500 font-medium">Session State</dt>
              <dd className="inline-flex items-center gap-1.5 text-xs text-emerald-700 font-medium mt-0.5">
                <CheckCircle2 size={13} aria-hidden="true" />
                <span>Active Spring Session (JDBC)</span>
              </dd>
            </div>
          </dl>
        </Card>

        <Card>
          <CardHeader
            title="School Information"
            subtitle="Assigned school identity"
            action={<Shield className="text-slate-400" size={20} />}
          />
          <dl className="space-y-2.5 text-sm">
            <div>
              <dt className="text-xs text-slate-500 font-medium">School Name</dt>
              <dd className="font-semibold text-slate-800">{school.name}</dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500 font-medium">DISE Code</dt>
              <dd className="font-mono text-slate-800">{school.schoolCode}</dd>
            </div>
            <div>
              <dt className="text-xs text-slate-500 font-medium">Established</dt>
              <dd className="text-slate-800">{school.establishedYear}</dd>
            </div>
          </dl>
        </Card>
      </div>
    </div>
  );
}
