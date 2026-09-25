import { Card } from "@/components/ui/Card";
import { Wrench } from "lucide-react";

export function AdminEkamKasotiTab() {
  return (
    <Card className="p-6 max-w-2xl text-center">
      <div className="flex flex-col items-center justify-center py-8">
        <Wrench size={48} className="text-slate-300 mb-4" />
        <h2 className="text-lg font-bold text-slate-900 mb-2">એકમ કસોટી પરિણામ મોડ્યુલ</h2>
        <p className="text-slate-600 text-sm max-w-sm">
          આ સુવિધા ટૂંક સમયમાં ઉપલબ્ધ કરવામાં આવશે. હાલમાં આ પેજનું નિર્માણ ચાલુ છે.
        </p>
      </div>
    </Card>
  );
}
