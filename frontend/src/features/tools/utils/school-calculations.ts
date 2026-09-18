export interface AttendanceResult {
  absentDays: number;
  percentage: number;
}

export interface MarksResult {
  percentage: number;
}

export interface SubjectMarks {
  obtained: number;
  maximum: number;
}

export interface MultiSubjectMarksResult {
  totalObtained: number;
  totalMaximum: number;
  overallPercentage: number;
  averagePercentage: number;
}

export function calculateAttendance(workingDays: number, presentDays: number): AttendanceResult | null {
  if (
    !Number.isInteger(workingDays) ||
    !Number.isInteger(presentDays) ||
    workingDays <= 0 ||
    presentDays < 0 ||
    presentDays > workingDays
  ) {
    return null;
  }

  return {
    absentDays: workingDays - presentDays,
    percentage: (presentDays / workingDays) * 100,
  };
}

export function calculateMarks(obtained: number, maximum: number): MarksResult | null {
  if (
    !Number.isFinite(obtained) ||
    !Number.isFinite(maximum) ||
    maximum <= 0 ||
    obtained < 0 ||
    obtained > maximum
  ) {
    return null;
  }

  return { percentage: (obtained / maximum) * 100 };
}

export function calculateMultiSubjectMarks(
  subjects: SubjectMarks[],
): MultiSubjectMarksResult | null {
  if (subjects.length === 0) return null;
  const percentages = subjects.map(({ obtained, maximum }) => calculateMarks(obtained, maximum));
  if (percentages.some((result) => result === null)) return null;

  const totalObtained = subjects.reduce((total, subject) => total + subject.obtained, 0);
  const totalMaximum = subjects.reduce((total, subject) => total + subject.maximum, 0);
  const overall = calculateMarks(totalObtained, totalMaximum);
  if (!overall) return null;

  return {
    totalObtained,
    totalMaximum,
    overallPercentage: overall.percentage,
    averagePercentage:
      percentages.reduce((total, result) => total + (result?.percentage ?? 0), 0) / percentages.length,
  };
}

export function formatPercentage(value: number): string {
  return `${value.toFixed(2)}%`;
}
