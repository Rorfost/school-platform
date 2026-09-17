import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Lock, ShieldAlert } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import schoolLogo from "@/assets/school-logo.jpeg";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { PasswordInput } from "@/components/ui/PasswordInput";
import { useAuth } from "@/features/auth/useAuth";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";

const loginSchema = z.object({
  email: z.string().trim().min(1, "Email is required").email("Please enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function AdminLoginPage() {
  const { login } = useAuth();
  const school = useEffectiveSchoolInfo();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as {
    from?: { pathname?: string };
    expired?: boolean;
  } | null;
  const from = locationState?.from?.pathname || "/admin";
  const isExpired = locationState?.expired ?? false;
  const [serverError, setServerError] = useState<string | null>(
    isExpired ? "Your session has expired. Please sign in again." : null,
  );

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      setServerError(null);
      await login(values.email, values.password);
      navigate(from, { replace: true });
    } catch (error) {
      if (error instanceof Error) {
        setServerError(error.message);
      } else {
        setServerError("Invalid credentials or server unavailable.");
      }
    }
  };

  return (
    <div className="flex min-h-screen flex-col justify-center bg-slate-100 px-4 py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <img
            src={school.logoUrl ?? schoolLogo}
            onError={(event) => {
              event.currentTarget.onerror = null;
              event.currentTarget.src = schoolLogo;
            }}
            alt="School Logo"
            className="size-16 rounded-full object-contain border border-slate-200 shadow-sm"
          />
        </div>
        <h1 className="mt-4 text-center text-2xl font-bold tracking-tight text-slate-900">
          Principal Admin Portal
        </h1>
        <p className="mt-1 text-center text-sm text-slate-600">{school.name}</p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="rounded-xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs">
          {serverError && (
            <div
              className="mb-5 flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-900"
              role="alert"
            >
              <ShieldAlert className="mt-0.5 shrink-0 text-red-600" size={18} aria-hidden="true" />
              <span>{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <Input
              label="Email Address"
              type="email"
              autoComplete="email"
              placeholder="principal@school.edu"
              error={errors.email?.message}
              {...register("email")}
            />

            <PasswordInput
              label="Password"
              autoComplete="current-password"
              placeholder="••••••••••••"
              error={errors.password?.message}
              {...register("password")}
            />

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                loading={isSubmitting}
                className="w-full justify-center"
              >
                <Lock size={16} aria-hidden="true" />
                <span>Sign in to Admin</span>
              </Button>
            </div>
          </form>
        </div>

        <div className="mt-6 text-center">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-blue-900 transition-colors"
          >
            <ArrowLeft size={16} aria-hidden="true" />
            <span>Back to Public Website</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
