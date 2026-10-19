import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Save, Trash2 } from "lucide-react";
import { apiRequest } from "@/api/client";
import type { GradeBand, ResultPresentationSettingsResponse } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import type { ResultSettingsTab } from "./AdminAssessmentsPage";

export function AdminResultPresentationSettings({
    settingsTab,
}: {
    settingsTab: ResultSettingsTab;
}) {
    const queryClient = useQueryClient();
    const settings = useQuery<ResultPresentationSettingsResponse>({
        queryKey: ["admin", "result-settings"],
        queryFn: () =>
            apiRequest<ResultPresentationSettingsResponse>("/api/v1/admin/result-settings"),
    });

    const [bands, setBands] = useState<GradeBand[] | null>(null);
    const [bandError, setBandError] = useState<string | null>(null);

    const save = useMutation({
        mutationFn: (payload: ResultPresentationSettingsResponse) =>
            apiRequest<ResultPresentationSettingsResponse>("/api/v1/admin/result-settings", {
                method: "PUT",
                body: payload,
            }),
        onSuccess: (value) => queryClient.setQueryData(["admin", "result-settings"], value),
    });

    if (!settings.data) return null;
    const value = settings.data;
    const isAnnual = settingsTab === "ANNUAL";

    const currentBands: GradeBand[] = bands ??
        value.gradeBands ?? [
            { label: "A", minPercentage: value.gradeAMin ?? 80 },
            { label: "B", minPercentage: value.gradeBMin ?? 65 },
            { label: "C", minPercentage: value.gradeCMin ?? 50 },
            { label: "D", minPercentage: value.gradeDMin ?? 35 },
            { label: "F", minPercentage: 0 },
        ];

    const validateBands = (list: GradeBand[]): string | null => {
        if (list.length === 0) return "At least one grade band is required.";
        const labels = new Set<string>();
        let hasZero = false;

        for (const b of list) {
            const trimmed = b.label.trim();
            if (!trimmed) return "Grade label cannot be empty.";
            if (trimmed.length > 20) return "Grade label must be 20 characters or fewer.";
            const upper = trimmed.toUpperCase();
            if (labels.has(upper)) return `Duplicate grade label: ${trimmed}`;
            labels.add(upper);

            if (b.minPercentage < 0 || b.minPercentage > 100) {
                return `Threshold must be between 0 and 100 for grade ${trimmed}.`;
            }
            if (b.minPercentage === 0) {
                if (hasZero) return "Only one catch-all grade band with 0% is allowed.";
                hasZero = true;
            }
        }
        if (!hasZero) return "A catch-all grade band with 0% minimum is required.";
        return null;
    };

    const handleBandChange = (index: number, field: keyof GradeBand, val: string | number) => {
        const next = [...currentBands];
        if (field === "minPercentage") {
            next[index] = { ...next[index], minPercentage: Number(val) };
        } else {
            next[index] = { ...next[index], label: String(val) };
        }
        setBands(next);
        setBandError(null);
    };

    const handleAddBand = () => {
        const next = [
            ...currentBands,
            { label: `Grade-${currentBands.length + 1}`, minPercentage: 10 },
        ];
        setBands(next);
        setBandError(null);
    };

    const handleRemoveBand = (index: number) => {
        if (currentBands.length <= 1) {
            setBandError("Cannot remove the only grade band.");
            return;
        }
        const next = currentBands.filter((_, i) => i !== index);
        setBands(next);
        setBandError(null);
    };

    return (
        <Card className="max-w-2xl">
            <h2 className="text-lg font-bold text-slate-900">
                {isAnnual ? "Exam Result Sheet Settings" : "Trimasik Kasoti Result Sheet Settings"}
            </h2>
            <form
                key={settingsTab}
                className="mt-4 space-y-4"
                onSubmit={(event) => {
                    event.preventDefault();
                    const form = new FormData(event.currentTarget);

                    if (isAnnual) {
                        const err = validateBands(currentBands);
                        if (err) {
                            setBandError(err);
                            return;
                        }
                    }

                    // Sort descending for backend normalized thresholds
                    const sortedBands = [...currentBands].sort(
                        (a, b) => b.minPercentage - a.minPercentage,
                    );
                    const gradeA = sortedBands.find((b) => b.label === "A")?.minPercentage ?? 80;
                    const gradeB = sortedBands.find((b) => b.label === "B")?.minPercentage ?? 65;
                    const gradeC = sortedBands.find((b) => b.label === "C")?.minPercentage ?? 50;
                    const gradeD = sortedBands.find((b) => b.label === "D")?.minPercentage ?? 35;

                    save.mutate({
                        resultDate: isAnnual
                            ? String(form.get("resultDate") || "") || null
                            : (value.resultDate ?? null),
                        resultPlace: isAnnual
                            ? String(form.get("resultPlace") || "") || null
                            : (value.resultPlace ?? null),
                        resultSheetTitle: isAnnual
                            ? String(form.get("resultSheetTitle") || "") || null
                            : (value.resultSheetTitle ?? null),
                        footerLineOne: isAnnual
                            ? String(form.get("footerLineOne") || "") || null
                            : (value.footerLineOne ?? null),
                        footerLineTwo: isAnnual
                            ? String(form.get("footerLineTwo") || "") || null
                            : (value.footerLineTwo ?? null),
                        ekamResultDate: isAnnual
                            ? (value.ekamResultDate ?? null)
                            : String(form.get("ekamResultDate") || "") || null,
                        ekamResultPlace: isAnnual
                            ? (value.ekamResultPlace ?? null)
                            : String(form.get("ekamResultPlace") || "") || null,
                        ekamResultSheetTitle: isAnnual
                            ? (value.ekamResultSheetTitle ?? null)
                            : String(form.get("ekamResultSheetTitle") || "") || null,
                        ekamFooterLineOne: isAnnual
                            ? (value.ekamFooterLineOne ?? null)
                            : String(form.get("ekamFooterLineOne") || "") || null,
                        ekamFooterLineTwo: isAnnual
                            ? (value.ekamFooterLineTwo ?? null)
                            : String(form.get("ekamFooterLineTwo") || "") || null,
                        gradeAMin: isAnnual ? gradeA : value.gradeAMin,
                        gradeBMin: isAnnual ? gradeB : value.gradeBMin,
                        gradeCMin: isAnnual ? gradeC : value.gradeCMin,
                        gradeDMin: isAnnual ? gradeD : value.gradeDMin,
                        gradeBands: isAnnual ? sortedBands : value.gradeBands,
                    });
                }}
            >
                {isAnnual ? (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            <Input
                                label="Result place (સ્થળ)"
                                name="resultPlace"
                                defaultValue={value.resultPlace ?? ""}
                                placeholder="દા.ત. ધધાણા"
                            />
                            <Input
                                label="Result date (તારીખ)"
                                name="resultDate"
                                type="date"
                                defaultValue={value.resultDate ?? ""}
                            />
                        </div>
                        <Input
                            label="Result sheet title (e.g. વાર્ષિક પરીક્ષા પરિણામ પત્રક : ૨૦૨૫-૨૬)"
                            name="resultSheetTitle"
                            defaultValue={value.resultSheetTitle ?? ""}
                            placeholder="વાર્ષિક પરીક્ષા પરિણામ પત્રક : ૨૦૨૫-૨૬"
                        />
                        <div className="space-y-3 border-t border-slate-100 pt-3">
                            <h3 className="text-sm font-bold text-slate-800">Footer notes</h3>
                            <label className="block text-sm font-medium text-slate-700">
                                Footer line 1
                                <textarea
                                    name="footerLineOne"
                                    defaultValue={value.footerLineOne ?? ""}
                                    className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                                    rows={2}
                                />
                            </label>
                            <label className="block text-sm font-medium text-slate-700">
                                Footer line 2
                                <textarea
                                    name="footerLineTwo"
                                    defaultValue={value.footerLineTwo ?? ""}
                                    className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                                    rows={2}
                                />
                            </label>
                        </div>

                        {/* Flexible Grade Policy Bands */}
                        <div className="space-y-3 border-t border-slate-100 pt-3">
                            <div className="flex items-center justify-between">
                                <div>
                                    <h3 className="text-sm font-bold text-slate-800">
                                        Flexible Grade Policy (ગ્રેડ નીતિ)
                                    </h3>
                                    <p className="text-xs text-slate-500">
                                        Configure grade labels and minimum percentages. Lowest band
                                        must be 0%.
                                    </p>
                                </div>
                                <Button
                                    type="button"
                                    variant="secondary"
                                    onClick={handleAddBand}
                                    className="gap-1 text-xs"
                                >
                                    <Plus size={14} /> Add Grade
                                </Button>
                            </div>

                            {bandError && (
                                <p className="text-xs text-red-600 font-semibold">{bandError}</p>
                            )}

                            <div className="space-y-2">
                                {currentBands.map((band, idx) => (
                                    <div
                                        key={idx}
                                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2"
                                    >
                                        <div className="w-1/2">
                                            <label className="text-[11px] font-medium text-slate-600 block">
                                                Grade Label
                                            </label>
                                            <Input
                                                value={band.label}
                                                onChange={(e) =>
                                                    handleBandChange(idx, "label", e.target.value)
                                                }
                                                placeholder="e.g. A+, A, B"
                                                required
                                            />
                                        </div>
                                        <div className="w-1/2">
                                            <label className="text-[11px] font-medium text-slate-600 block">
                                                Minimum %
                                            </label>
                                            <Input
                                                type="number"
                                                min={0}
                                                max={100}
                                                value={band.minPercentage}
                                                onChange={(e) =>
                                                    handleBandChange(
                                                        idx,
                                                        "minPercentage",
                                                        e.target.value,
                                                    )
                                                }
                                                required
                                            />
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveBand(idx)}
                                            className="mt-4 p-2 text-slate-400 hover:text-red-600 transition"
                                            title="Remove grade band"
                                            aria-label={`Remove grade ${band.label}`}
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </>
                ) : (
                    <>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            <Input
                                label="Result place (સ્થળ)"
                                name="ekamResultPlace"
                                defaultValue={value.ekamResultPlace ?? ""}
                                placeholder="દા.ત. ધધાણા"
                            />
                            <Input
                                label="Result date (તારીખ)"
                                name="ekamResultDate"
                                type="date"
                                defaultValue={value.ekamResultDate ?? ""}
                            />
                        </div>
                        <Input
                            label="Result sheet title (e.g. ત્રિમાસિક કસોટી પરિણામ પત્રક : ૨૦૨૫-૨૬)"
                            name="ekamResultSheetTitle"
                            defaultValue={value.ekamResultSheetTitle ?? ""}
                            placeholder="ત્રિમાસિક કસોટી પરિણામ પત્રક : ૨૦૨૫-૨૬"
                        />
                        <div className="space-y-3 border-t border-slate-100 pt-3">
                            <h3 className="text-sm font-bold text-slate-800">Footer notes</h3>
                            <label className="block text-sm font-medium text-slate-700">
                                Footer line 1
                                <textarea
                                    name="ekamFooterLineOne"
                                    defaultValue={value.ekamFooterLineOne ?? ""}
                                    className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                                    rows={2}
                                />
                            </label>
                            <label className="block text-sm font-medium text-slate-700">
                                Footer line 2
                                <textarea
                                    name="ekamFooterLineTwo"
                                    defaultValue={value.ekamFooterLineTwo ?? ""}
                                    className="mt-1 w-full rounded-lg border border-slate-300 p-2 text-sm"
                                    rows={2}
                                />
                            </label>
                        </div>
                    </>
                )}
                <Button
                    type="submit"
                    loading={save.isPending}
                    loadingText="Saving result settings..."
                >
                    <Save size={16} /> Save Result Settings
                </Button>
            </form>
        </Card>
    );
}
