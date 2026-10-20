import { api } from './client';

/** Типы повторяют DTO analytics-service. BigDecimal приходит из Jackson числом. */
export type ExerciseStatus = 'OVERLOAD' | 'PLATEAU' | 'RECORD' | 'NORMAL';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type ProgressPeriod = 'M1' | 'M3' | 'M6' | 'Y1' | 'ALL';

export interface ExerciseSummary {
  exerciseId: string;
  exerciseName: string;
  lastPerformedAt: string;
  lastWeightKg: number | null;
  lastReps: number | null;
  currentE1rmKg: number | null;
  status: ExerciseStatus;
  riskLevel: RiskLevel;
  note: string | null;
  /** последние значения силы (1ПМ) по тренировкам — для мини-графика */
  trend: number[];
}

export interface Dashboard {
  empty: boolean;
  month: string; // "2026-10"
  workoutsThisMonth: number;
  weekTonnageKg: number;
  previousWeekTonnageKg: number;
  weekTonnageChangePercent: number | null;
  focusExerciseId: string | null;
  exercises: ExerciseSummary[];
}

export interface ProgressPoint {
  date: string;
  e1rmKg: number | null;
  bestWeightKg: number | null;
  bestReps: number | null;
  volumeKg: number;
  referenceE1rmKg: number | null;
}

export interface ExerciseProgress {
  exerciseId: string;
  exerciseName: string;
  period: ProgressPeriod;
  currentE1rmKg: number | null;
  changePercent: number | null;
  weeklyVolumeKg: number | null;
  points: ProgressPoint[];
}

export interface ExerciseInsights {
  exerciseId: string;
  exerciseName: string | null;
  sessionsCount: number;
  plateau: { detected: boolean; lastProgressAt: string | null; daysSinceProgress: number };
  overloadRisk: { level: RiskLevel; reasons: string[] };
  recommendation: { type: 'NOT_ENOUGH_DATA' | 'KEEP_GOING' | 'WATCH_RECOVERY' | 'DELOAD'; message: string };
}

/** План разгрузочной недели (analytics: DeloadPlanResponse) */
export interface DeloadPlan {
  exerciseId: string;
  exerciseName: string;
  reason: string;
  durationDays: number;
  intensityPercent: number;
  currentE1rmKg: number | null;
  workingWeightKg: number | null;
  sets: number;
  reps: number;
  notes: string[];
}

export const analyticsApi = {
  dashboard: () => api<Dashboard>('/api/analytics/dashboard'),
  progress: (exerciseId: string, period: ProgressPeriod = 'M3') =>
    api<ExerciseProgress>(`/api/analytics/exercises/${exerciseId}/progress?period=${period}`),
  insights: (exerciseId: string) => api<ExerciseInsights>(`/api/analytics/exercises/${exerciseId}/insights`),
  deloadPlan: (exerciseId: string) => api<DeloadPlan>(`/api/analytics/exercises/${exerciseId}/deload-plan`),
};
