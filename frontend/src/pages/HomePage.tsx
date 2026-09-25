import {
  ArrowRight,
  BookOpen,
  Calendar,
  Download,
  FileText,
  GraduationCap,
  Mail,
  MapPin,
  Newspaper,
} from "lucide-react";
import { Link } from "react-router-dom";
import { usePublicNotices } from "@/features/public/usePublicContent";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { ContentSkeleton, ErrorState, LoadingState } from "@/components/common/StatusPanel";
import { useEffectiveSchoolInfo } from "@/features/school/useSchoolData";
import { LABELS } from "@/utils/gujarati";

export function HomePage() {
  const school = useEffectiveSchoolInfo();
  const {
    data: noticesData,
    isLoading: isNoticesLoading,
    error: noticesError,
    refetch: refetchNotices,
  } = usePublicNotices(0, 3);
  const recentNotices = noticesData?.items ?? [];

  const studentShortcuts = [
    {
      to: "/student/materials",
      title: LABELS.materials,
      description: "ધોરણ ૧ થી ૮ માટે વિષયવાર પાઠ્યપુસ્તકો અને સ્વાધ્યાય સામગ્રી",
      icon: BookOpen,
      badgeText: "અભ્યાસ",
      badgeVariant: "primary" as const,
    },
    {
      to: "/student/results",
      title: LABELS.results,
      description: "એકમ કસોટી અને પરીક્ષા પરિણામ અંગેની માહિતી",
      icon: GraduationCap,
      badgeText: "પરિણામ",
      badgeVariant: "secondary" as const,
    },
    {
      to: "/student/downloads",
      title: LABELS.downloads,
      description: "શાળાના ઉપયોગી ફોર્મ, પત્રકો અને અરજી નમૂના",
      icon: Download,
      badgeText: "ડાઉનલોડ",
      badgeVariant: "neutral" as const,
    },
    {
      to: "/notices",
      title: LABELS.notices,
      description: "શાળાની તાજી જાહેરાતો અને અગત્યની સૂચનાઓ",
      icon: Newspaper,
      badgeText: "સૂચના",
      badgeVariant: "accent" as const,
    },
  ];

  return (
    <div className="space-y-8 sm:space-y-12">
      {/* Student Quick Access Section */}
      <section aria-labelledby="student-corner-heading">
        <div className="flex items-center justify-between gap-2 sm:gap-4 mb-4">
          <div className="min-w-0 flex-1">
            <h2
              id="student-corner-heading"
              className="text-lg sm:text-2xl font-bold tracking-tight text-slate-900 truncate"
            >
              {LABELS.studentCorner}
            </h2>
            <p className="text-xs sm:text-sm text-slate-600 mt-0.5 truncate">
              વિદ્યાર્થીઓ અને વાલીઓ માટે જરૂરી વિભાગો
            </p>
          </div>
          <Link
            to="/student"
            className="inline-flex shrink-0 items-center gap-1 text-xs sm:text-sm font-semibold text-blue-900 hover:text-blue-950"
          >
            <span>બધું જુઓ</span>
            <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {studentShortcuts.map((item) => {
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                className="group flex w-full min-w-0 min-h-44 flex-col justify-between rounded-xl border border-slate-200 bg-white p-4 shadow-xs transition-all hover:border-blue-300 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900 sm:p-5"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex size-11 items-center justify-center rounded-lg bg-blue-50 text-blue-900 group-hover:bg-blue-900 group-hover:text-white transition-colors">
                      <Icon size={22} aria-hidden="true" />
                    </div>
                    <Badge variant={item.badgeVariant} size="sm">
                      {item.badgeText}
                    </Badge>
                  </div>
                  <h3 className="text-base font-bold text-slate-900 group-hover:text-blue-900 transition-colors">
                    {item.title}
                  </h3>
                  <p className="mt-1.5 text-xs sm:text-sm text-slate-600 leading-relaxed">
                    {item.description}
                  </p>
                </div>
                <div className="mt-5 flex items-center gap-1 text-xs font-semibold text-blue-900">
                  <span>પ્રવેશ કરો</span>
                  <ArrowRight
                    size={13}
                    className="transition-transform group-hover:translate-x-1"
                    aria-hidden="true"
                  />
                </div>
              </Link>
            );
          })}
        </div>
      </section>

      {/* Notice Board Preview */}
      <section aria-labelledby="notices-heading">
        <div className="flex items-center justify-between gap-2 sm:gap-4 mb-4">
          <div className="min-w-0 flex-1">
            <h2
              id="notices-heading"
              className="text-lg sm:text-2xl font-bold tracking-tight text-slate-900 truncate"
            >
              {LABELS.notices}
            </h2>
            <p className="text-xs sm:text-sm text-slate-600 mt-0.5 truncate">
              શાળાની તાજેતરની જાહેરાતો
            </p>
          </div>
          <Link
            to="/notices"
            className="inline-flex shrink-0 items-center gap-1 text-xs sm:text-sm font-semibold text-blue-900 hover:text-blue-950"
          >
            <span>તમામ સૂચનાઓ</span>
            <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>

        {isNoticesLoading ? (
          <>
            <LoadingState
              message="સૂચનાઓ લોડ થઈ રહી છે..."
              delayedMessage="થોડો સમય લાગી શકે છે."
            />
            <ContentSkeleton rows={2} />
          </>
        ) : noticesError ? (
          <ErrorState onRetry={() => refetchNotices()} />
        ) : recentNotices.length > 0 ? (
          <div className="space-y-3">
            {recentNotices.map((notice) => (
              <Card key={notice.id} className="p-4 sm:p-5">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                  <div className="flex items-center gap-2.5">
                    <FileText className="text-blue-900 shrink-0" size={18} aria-hidden="true" />
                    <h3 className="text-base font-semibold text-slate-900">{notice.title}</h3>
                    {notice.pinned && (
                      <Badge variant="primary" size="sm">
                        મહત્વપૂર્ણ
                      </Badge>
                    )}
                  </div>
                  {notice.expiresAt && (
                    <div className="flex items-center gap-1 text-xs text-slate-500 shrink-0">
                      <Calendar size={13} aria-hidden="true" />
                      <span>મુદત: {notice.expiresAt.slice(0, 10)}</span>
                    </div>
                  )}
                </div>
                <p className="mt-2 text-sm text-slate-600 line-clamp-2 leading-relaxed">
                  {notice.body}
                </p>
              </Card>
            ))}
          </div>
        ) : (
          <Card className="p-6 text-center text-slate-600">
            <p className="text-sm font-medium">હાલ કોઈ નવી સૂચના નથી.</p>
            <p className="text-xs text-slate-500 mt-1">નવી સૂચનાઓ જાહેર થતાં જ અહીં જોવા મળશે.</p>
          </Card>
        )}
      </section>

      {/* School Highlights & Contact Banner */}
      <section
        aria-labelledby="contact-summary-heading"
        className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs"
      >
        <h2
          id="contact-summary-heading"
          className="text-lg sm:text-xl font-bold text-slate-900 mb-4"
        >
          શાળા સંપર્ક અને સ્થાન
        </h2>
        <div className="grid grid-cols-1 gap-6 text-sm md:grid-cols-2">
          <div className="flex items-start gap-3">
            <MapPin className="text-blue-900 shrink-0 mt-1" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-800">સરનામું</p>
              <p className="text-slate-600 mt-0.5">{school.address}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Mail className="text-blue-900 shrink-0 mt-1" size={20} aria-hidden="true" />
            <div>
              <p className="font-semibold text-slate-800">ઈમેઈલ</p>
              <a
                href={`mailto:${school.email}`}
                className="text-blue-900 hover:underline mt-0.5 block break-all"
              >
                {school.email}
              </a>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
