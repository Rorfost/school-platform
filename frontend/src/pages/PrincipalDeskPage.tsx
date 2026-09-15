import { Mail, Phone, UserCheck } from "lucide-react";
import { LoadingState } from "@/components/common/StatusPanel";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/Card";
import { usePublicPrincipalProfile } from "@/features/school/useSchoolData";
import { LABELS } from "@/utils/gujarati";

export function PrincipalDeskPage() {
  const { data: profile, isLoading } = usePublicPrincipalProfile();

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={LABELS.principalDesk}
          backTo="/"
          backLabel={LABELS.home}
        />
        <LoadingState message="આચાર્યશ્રીની વિગતો લોડ થઈ રહી છે..." />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <PageHeader
        title={LABELS.principalDesk}
        description="શાળા આચાર્યશ્રી તરફથી વિદ્યાર્થીઓ અને વાલીઓ માટે સંદેશ"
        backTo="/"
        backLabel={LABELS.home}
      />

      <Card className="p-6 sm:p-8">
        <div className="flex items-start gap-4">
          <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-900 border border-blue-200">
            <UserCheck size={28} aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-900">
              {profile?.fullName || "આચાર્યશ્રી"}
            </h2>
            <p className="text-sm font-medium text-slate-600">
              {profile?.designation || "મુખ્ય શિક્ષક / આચાર્ય"}
            </p>
            {profile?.qualification && (
              <p className="text-xs text-slate-500 mt-0.5">
                લાયકાત: {profile.qualification}
              </p>
            )}
          </div>
        </div>

        <div className="mt-6 border-t border-slate-100 pt-6">
          <h3 className="text-base font-semibold text-slate-900 mb-2">સંદેશ</h3>
          <p className="text-sm sm:text-base leading-relaxed text-slate-700 whitespace-pre-line">
            {profile?.message ||
              "પ્રિય વિદ્યાર્થીઓ અને વાલીશ્રીઓ,\n\nઅમારી શાળામાં આપ સૌનું હાર્દિક સ્વાગત છે. પ્રાથમિક શિક્ષણ એ બાળકનું ઘડતર કરવાનો પાયો છે. અમારો સતત પ્રયાસ રહ્યો છે કે દરેક બાળક નિર્ભય વાતાવરણમાં આનંદ સાથે શિક્ષણ મેળવે અને પોતાના જીવનમાં ઉત્કૃષ્ટ બને.\n\nઆ પોર્ટલ દ્વારા શાળાની શૈક્ષણિક પ્રવૃત્તિઓ, અભ્યાસ સામગ્રી અને જરૂરી સૂચનાઓ સરળતાથી આપ સુધી પહોંચાડવામાં આવશે."}
          </p>
        </div>

        {profile?.isContactPublic && (profile.email || profile.phone) && (
          <div className="mt-6 border-t border-slate-100 pt-4 flex flex-wrap gap-4 text-xs sm:text-sm text-slate-600">
            {profile.email && (
              <div className="flex items-center gap-1.5">
                <Mail size={15} className="text-blue-900" aria-hidden="true" />
                <a href={`mailto:${profile.email}`} className="hover:underline">
                  {profile.email}
                </a>
              </div>
            )}
            {profile.phone && (
              <div className="flex items-center gap-1.5">
                <Phone size={15} className="text-blue-900" aria-hidden="true" />
                <span>{profile.phone}</span>
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}
