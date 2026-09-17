import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, KeyRound, LogOut, ShieldCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { apiRequest } from "@/api/client";
import type { PasswordChangeRequest, PrincipalAccountResponse } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { PasswordInput } from "@/components/ui/PasswordInput";
import { useAuth } from "@/features/auth/useAuth";

export function AdminAccountPage() {
  const { principal, logout } = useAuth();
  const navigate = useNavigate();
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const passwordMutation = useMutation({
    mutationFn: (payload: PasswordChangeRequest) =>
      apiRequest<PrincipalAccountResponse>("/api/v1/admin/auth/password", {
        method: "PUT",
        body: payload,
      }),
    onSuccess: () => {
      setSuccessMsg("Password changed successfully. Your security status is now clean.");
      setErrorMsg(null);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    },
    onError: (err) => {
      setErrorMsg(err instanceof Error ? err.message : "Failed to change password.");
      setSuccessMsg(null);
    },
  });

  const handlePasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (newPassword.length < 12) {
      setErrorMsg("New password must be at least 12 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMsg("New password and confirm password do not match.");
      return;
    }

    passwordMutation.mutate({
      currentPassword,
      newPassword,
    });
  };

  const handleLogout = async () => {
    await logout();
    navigate("/admin/login");
  };

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Account Security & Credentials</h1>
        <p className="text-sm text-slate-600 mt-1">
          Manage your Principal session credentials, role authorization, and account security.
        </p>
      </div>

      {/* Account Info Card */}
      <Card className="p-6 space-y-4">
        <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
          <ShieldCheck size={18} className="text-blue-900" />
          <span>Active Principal Account</span>
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
          <div>
            <span className="text-xs font-semibold text-slate-500 block uppercase">
              Email Address
            </span>
            <span className="font-bold text-slate-900">
              {principal?.email || "principal@school.in"}
            </span>
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-500 block uppercase">Role</span>
            <span className="font-bold text-blue-900">{principal?.role || "PRINCIPAL"}</span>
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-500 block uppercase">
              Must Change Password
            </span>
            <span
              className={`font-bold ${
                principal?.mustChangePassword ? "text-amber-600" : "text-emerald-700"
              }`}
            >
              {principal?.mustChangePassword ? "Yes (Action Required)" : "No (Secure)"}
            </span>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-100 flex justify-end">
          <Button
            variant="outline"
            size="sm"
            onClick={handleLogout}
            className="gap-2 text-slate-700"
          >
            <LogOut size={15} />
            <span>Sign Out of Session</span>
          </Button>
        </div>
      </Card>

      {/* Password Change Form */}
      <Card className="p-6 sm:p-8 space-y-4">
        <h2 className="text-base font-bold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
          <KeyRound size={18} className="text-blue-900" />
          <span>Change Account Password</span>
        </h2>

        {successMsg && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-900 text-sm font-semibold flex items-center gap-2">
            <CheckCircle2 size={18} className="text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        {errorMsg && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-900 text-sm font-medium flex items-center gap-2">
            <AlertCircle size={18} className="text-red-600 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handlePasswordSubmit} className="space-y-4 max-w-md">
          <PasswordInput
            label="Current Password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />

          <PasswordInput
            label="New Password (min 12 characters)"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            helperText="Password must be between 12 and 72 characters long."
          />

          <PasswordInput
            label="Confirm New Password"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />

          <Button
            type="submit"
            variant="primary"
            className="w-full gap-2 mt-2"
            loading={passwordMutation.isPending}
          >
            <KeyRound size={16} aria-hidden="true" />
            <span>Update Password</span>
          </Button>
        </form>
      </Card>
    </div>
  );
}
