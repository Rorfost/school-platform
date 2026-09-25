import { useState } from "react";
import { UploadCloud, CheckCircle2 } from "lucide-react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { apiRequest } from "@/api/client";
import { useMutation } from "@tanstack/react-query";

export function AdminExamResultsTab() {
  const [file, setFile] = useState<File | null>(null);
  const [totalWorkingDays, setTotalWorkingDays] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const uploadMutation = useMutation({
    mutationFn: async () => {
      const formData = new FormData();
      formData.append("file", file!);
      formData.append("totalWorkingDays", totalWorkingDays);
      return apiRequest("/api/v1/admin/exam-results/upload", {
        method: "POST",
        body: formData,
      });
    },
    onSuccess: () => {
      setSuccessMsg("પરીક્ષા પરિણામ સફળતાપૂર્વક અપલોડ થઈ ગયું છે.");
      setFile(null);
      setTotalWorkingDays("");
      setTimeout(() => setSuccessMsg(""), 5000);
    },
  });

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !totalWorkingDays) return;
    uploadMutation.mutate();
  };

  return (
    <div className="space-y-6">
      <Card className="p-6 max-w-2xl">
        <h2 className="text-lg font-bold text-slate-900 mb-4">વાર્ષિક પરીક્ષા પરિણામ અપલોડ કરો</h2>
        
        {successMsg && (
          <div className="mb-4 flex items-center gap-2 rounded-lg bg-emerald-50 p-4 text-sm font-semibold text-emerald-900">
            <CheckCircle2 size={18} className="text-emerald-600" />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleUpload} className="space-y-4">
          <Input
            label="કુલ હાજર (કાર્ય) દિવસ"
            type="number"
            value={totalWorkingDays}
            onChange={(e) => setTotalWorkingDays(e.target.value)}
            placeholder="દા.ત. 230"
            required
          />
          
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              એક્સેલ ફાઈલ (Excel Sheet)
            </label>
            <input
              type="file"
              accept=".xlsx,.xls"
              required
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="block w-full text-sm text-slate-500
                file:mr-4 file:py-2 file:px-4
                file:rounded-lg file:border-0
                file:text-sm file:font-semibold
                file:bg-blue-50 file:text-blue-900
                hover:file:bg-blue-100"
            />
          </div>

          <Button
            type="submit"
            variant="primary"
            className="gap-2"
            disabled={!file || !totalWorkingDays}
            loading={uploadMutation.isPending}
          >
            <UploadCloud size={18} /> પરિણામ અપલોડ કરો
          </Button>

          {uploadMutation.isError && (
            <p className="text-sm text-red-600 font-medium">
              અપલોડ નિષ્ફળ ગયું. કૃપા કરીને ફાઈલનું ફોર્મેટ તપાસો.
            </p>
          )}
        </form>
      </Card>
    </div>
  );
}
