import {
  Award,
  BookOpen,
  Calendar,
  Camera,
  FileText,
  FolderDown,
  GraduationCap,
  Layers,
  Settings,
  UserCheck,
} from "lucide-react";
import { Link } from "react-router-dom";
import { usePublicGalleryAlbums, usePublicNotices } from "@/features/public/usePublicContent";
import { useEffectiveSchoolInfo, usePublicPrincipalProfile } from "@/features/school/useSchoolData";
import { Card } from "@/components/ui/Card";

export function AdminDashboardPage() {
  const school = useEffectiveSchoolInfo();
  const { data: profile } = usePublicPrincipalProfile();
  const { data: noticesData } = usePublicNotices(0, 10);
  const { data: galleryData } = usePublicGalleryAlbums(0, 10);

  const noticesCount = noticesData?.totalItems ?? 0;
  const galleryCount = galleryData?.totalItems ?? 0;

  const quickLinks = [
    { to: "/admin/school", label: "School Identity", icon: Settings, desc: "Name, address, contact, DISE code" },
    { to: "/admin/principal", label: "Principal Profile", icon: UserCheck, desc: "Biography, message, designation" },
    { to: "/admin/academic-years", label: "Academic Years", icon: Calendar, desc: "Manage sessions, current year" },
    { to: "/admin/standards", label: "Standards & Classes", icon: Layers, desc: "Standards 1 to 8 configuration" },
    { to: "/admin/subjects", label: "Subjects Catalog", icon: BookOpen, desc: "Curriculum subjects list" },
    { to: "/admin/assessments", label: "Assessments", icon: Award, desc: "Ekam Kasoti & exam setup" },
    { to: "/admin/materials", label: "Study Materials", icon: GraduationCap, desc: "Worksheets & textbook files" },
    { to: "/admin/notices", label: "Notices & Circulars", icon: FileText, desc: "School announcements" },
    { to: "/admin/gallery", label: "Photo Gallery", icon: Camera, desc: "Event albums & photos" },
    { to: "/admin/downloads", label: "Downloads", icon: FolderDown, desc: "Public forms & documents" },
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
              Welcome back, {profile?.fullName || "Principal"}
            </h1>
            <p className="text-sm text-slate-300 mt-1">
              {school.name} — DISE Code: {school.schoolCode || "24030401801"}
            </p>
          </div>
          <Link
            to="/admin/account"
            className="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-xs sm:text-sm font-semibold transition-colors shrink-0"
          >
            Manage Security & Password
          </Link>
        </div>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase">Published Notices</span>
            <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
              <FileText size={18} aria-hidden="true" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-slate-900 mt-2">{noticesCount}</p>
          <Link to="/admin/notices" className="text-xs font-semibold text-blue-900 hover:underline mt-1 block">
            Manage notices →
          </Link>
        </Card>

        <Card className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase">Photo Albums</span>
            <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
              <Camera size={18} aria-hidden="true" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-slate-900 mt-2">{galleryCount}</p>
          <Link to="/admin/gallery" className="text-xs font-semibold text-blue-900 hover:underline mt-1 block">
            Manage gallery →
          </Link>
        </Card>

        <Card className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase">Established Year</span>
            <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
              <Calendar size={18} aria-hidden="true" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-slate-900 mt-2">{school.establishedYear || 1950}</p>
          <span className="text-xs text-slate-500 mt-1 block">Primary School (Std 1–8)</span>
        </Card>

        <Card className="p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase">Medium</span>
            <div className="flex size-9 items-center justify-center rounded-lg bg-blue-50 text-blue-900">
              <BookOpen size={18} aria-hidden="true" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-slate-900 mt-2">{(school as { medium?: string }).medium || "Gujarati"}</p>
          <span className="text-xs text-slate-500 mt-1 block">Public Portal active</span>
        </Card>
      </div>

      {/* Operational Management Shortcuts Grid */}
      <div>
        <h2 className="text-lg font-bold text-slate-900 mb-4">Operational Management</h2>
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
    </div>
  );
}
