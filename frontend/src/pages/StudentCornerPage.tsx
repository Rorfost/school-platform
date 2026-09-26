import { BookOpen, Calculator, Download, GraduationCap } from "lucide-react";
import { Link } from "react-router-dom";
import { PageHeader } from "@/components/common/PageHeader";
import { Badge } from "@/components/ui/Badge";
import { LABELS } from "@/utils/gujarati";

export function StudentCornerPage() {
  const sections = [
    {
      to: "/student/materials",
      title: LABELS.materials,
      description:
        "ધોરણ ૧ થી ૮ માટે વિષયવાર પાઠ્યપુસ્તકો, સ્વાધ્યાય સામગ્રી અને શીખવા માટે ઉપયોગી ફાઈલો.",
      icon: BookOpen,
      badgeText: "અભ્યાસ",
    },
    {
      to: "/student/results",
      title: LABELS.results,
      description: "એકમ કસોટી, સત્રાંત કસોટી અને વાર્ષિક પરીક્ષા પરિણામ અંગેની માહિતી.",
      icon: GraduationCap,
      badgeText: "પરીક્ષા",
    },
    {
      to: "/student/downloads",
      title: LABELS.downloads,
      description: "શાળાના ફોર્મ્સ, પરિપત્રો અને વિદ્યાર્થીઓ માટે જરૂરી પત્રકો.",
      icon: Download,
      badgeText: "ફાઈલો",
    },
    {
      to: "/tools",
      title: LABELS.tools,
      description: "ઉંમર, તારીખ, હાજરી, ગુણ, પાડા અને નંબરને શબ્દોમાં લખવા માટેના સરળ સાધનો.",
      icon: Calculator,
      badgeText: "ઉપયોગી",
    },
  ];

  return (
    <div className="space-y-8">
      <PageHeader
        title={LABELS.studentCorner}
        description="ધોરણ ૧ થી ૮ ના વિદ્યાર્થીઓ અને વાલીઓ માટે તમામ શૈક્ષણિક સુવિધાઓ"
        backTo="/"
        backLabel={LABELS.home}
      />

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {sections.map((section) => {
          const Icon = section.icon;
          return (
            <Link
              key={section.to}
              to={section.to}
              className="group flex flex-col justify-between rounded-xl border border-slate-200 bg-white p-6 shadow-xs transition-all hover:border-[#0d2461] hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#0d2461]"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex size-12 items-center justify-center rounded-xl bg-blue-50 text-[#0d2461] group-hover:bg-[#0d2461] group-hover:text-white transition-colors">
                    <Icon size={24} aria-hidden="true" />
                  </div>
                  <Badge variant="primary" size="sm">
                    {section.badgeText}
                  </Badge>
                </div>
                <h2 className="text-lg font-bold text-slate-900 group-hover:text-[#0d2461] transition-colors">
                  {section.title}
                </h2>
                <p className="mt-2 text-sm text-slate-600 leading-relaxed">{section.description}</p>
              </div>
              <div className="mt-6 text-sm font-semibold text-blue-900">વિભાગ ખોલો →</div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
