import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ExamResultViewer } from "@/features/public/ExamResultViewer";
import type { ExamResultResponse } from "@/api/types";

function renderViewer(props: Parameters<typeof ExamResultViewer>[0]) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <ExamResultViewer {...props} />
        </QueryClientProvider>,
    );
}

describe("ExamResultViewer", () => {
    const syntheticAnnualResult: ExamResultResponse = {
        id: "res-001",
        schoolId: "sch-001",
        studentName: "વિશાલ કે. પટેલ",
        standard: "5",
        studentClass: "A",
        rollNumber: 1,
        generalRegisterNumber: "GR-101",
        birthDate: "10-05-2015",
        totalWorkingDays: 180,
        attendedDays: 165,
        totalMarks: 400,
        obtainedMarks: 320,
        percentage: 80,
        overallGrade: "A",
        subjects: [
            {
                id: "sub-1",
                subjectName: "ગુજરાતી",
                maximumMarks: 200,
                obtainedMarks: 160,
                grade: "A",
                status: "PRESENT",
                sortOrder: 1,
            },
            {
                id: "sub-2",
                subjectName: "ગણિત",
                maximumMarks: 200,
                obtainedMarks: 160,
                grade: "A",
                status: "PRESENT",
                sortOrder: 2,
            },
        ],
    };

    it("renders annual marksheet header and student details", () => {
        renderViewer({
            result: syntheticAnnualResult,
            schoolName: "શ્રી ધાણધાર પ્રાથમિક શાળા",
            logoUrl: "https://example.com/logo.png",
            sealUrl: "https://example.com/seal.png",
            resultType: "ANNUAL",
        });

        expect(screen.getByText("Print Result")).toBeInTheDocument();
        expect(screen.getByText("વિશાલ કે. પટેલ")).toBeInTheDocument();
        expect(screen.getByText("GR-101")).toBeInTheDocument();
        expect(screen.getByText("ગુજરાતી")).toBeInTheDocument();
        expect(screen.getByText("ગણિત")).toBeInTheDocument();
    });

    it("displays absent status for subjects marked absent", () => {
        const resultWithAbsent: ExamResultResponse = {
            ...syntheticAnnualResult,
            subjects: [
                {
                    id: "sub-1",
                    subjectName: "અંગ્રેજી",
                    maximumMarks: 200,
                    obtainedMarks: 0,
                    grade: null,
                    status: "ABSENT",
                    sortOrder: 1,
                },
            ],
        };

        renderViewer({
            result: resultWithAbsent,
            schoolName: "શ્રી ધાણધાર પ્રાથમિક શાળા",
            resultType: "ANNUAL",
        });

        expect(screen.getByText("અંગ્રેજી")).toBeInTheDocument();
        expect(screen.getByText("AB")).toBeInTheDocument();
    });

    it("renders Ekam Kasoti marksheet view without error", () => {
        renderViewer({
            result: syntheticAnnualResult,
            schoolName: "શ્રી ધાણધાર પ્રાથમિક શાળા",
            resultType: "EKAM_KASOTI",
        });

        expect(screen.getByText("વિશાલ કે. પટેલ")).toBeInTheDocument();
        expect(screen.getByText("ગુજરાતી")).toBeInTheDocument();
    });

    it("renders long student name and multiple subjects cleanly without crashing", () => {
        const longNameResult: ExamResultResponse = {
            ...syntheticAnnualResult,
            studentName: "ચૌધરી હિતેન્દ્રકુમાર પરબતભાઈ ઉર્ફે મોહનભાઈ રણછોડભાઈ",
            subjects: [
                ...syntheticAnnualResult.subjects,
                {
                    id: "sub-3",
                    subjectName: "વિજ્ઞાન અને ટેકનોલોજી",
                    maximumMarks: 200,
                    obtainedMarks: 175,
                    grade: "A+",
                    status: "PRESENT",
                    sortOrder: 3,
                },
                {
                    id: "sub-4",
                    subjectName: "સામાજિક વિજ્ઞાન",
                    maximumMarks: 200,
                    obtainedMarks: 155,
                    grade: "B",
                    status: "PRESENT",
                    sortOrder: 4,
                },
            ],
        };

        renderViewer({
            result: longNameResult,
            schoolName: "શ્રી મોડેલ પ્રાથમિક શાળા, પાટણ",
            logoUrl: null,
            sealUrl: null,
            resultType: "ANNUAL",
        });

        expect(
            screen.getByText("ચૌધરી હિતેન્દ્રકુમાર પરબતભાઈ ઉર્ફે મોહનભાઈ રણછોડભાઈ"),
        ).toBeInTheDocument();
        expect(screen.getByText("વિજ્ઞાન અને ટેકનોલોજી")).toBeInTheDocument();
        expect(screen.getByText("સામાજિક વિજ્ઞાન")).toBeInTheDocument();
    });
});
