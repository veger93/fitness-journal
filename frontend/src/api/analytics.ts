import { api } from './client';

/** Типы повторяют DTO analytics-service. BigDecimal приходит из Jackson числом. */
export type ExerciseStatus = 'OVERLOAD' | 'PLATEAU' | 'RECORD' | 'NORMAL';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type ProgressPeriod = 'M1' | 'M3' | 'M6' | 'Y1' | 'ALL';

export interface ExerciseSummary {
  exerciseId: string;
  exerciseName: string;
  trackingType: 'WEIGHT_REPS' | 'TIME' | 'DISTANCE';
  lastPerformedAt: string;
  lastWeightKg: number | null;
  lastReps: number | null;
  lastDistanceM: number | null;
  lastDurationSec: number | null;
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
  workingSets: number;
  /** лучшее время подхода — у упражнений на время (планка); у силовых null */
  bestDurationSec: number | null;
}

/** Сравнение с обычным темпом роста для уровня спортсмена (пунктир на графике) */
export interface ProgressReference {
  level: string;
  levelAssumed: boolean;
  monthlyGainPercent: number;
  gapPercent: number;
  hint: string;
}

export interface ExerciseProgress {
  exerciseId: string;
  exerciseName: string;
  period: ProgressPeriod;
  currentE1rmKg: number | null;
  changePercent: number | null;
  weeklyVolumeKg: number | null;
  reference: ProgressReference | null;
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

/** Карточка в ленте рекордов (analytics: PersonalRecordResponse) */
export interface PersonalRecord {
  /** тренировка, в которой поставлен рекорд — для значка в истории */
  workoutId: string;
  exerciseId: string;
  exerciseName: string;
  achievedAt: string;
  weightKg: number | null;
  reps: number | null;
  records: {
    type: 'WEIGHT' | 'E1RM';
    valueKg: number;
    deltaKg: number | null;
    hint: string | null;
  }[];
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

/** Страница "Бег" (analytics: RunningSummaryResponse) */
export interface RunRecord {
  date: string;
  distanceM: number | null;
  durationSec: number | null;
  paceSecPerKm: number | null;
  fresh: boolean;
}

export interface RunningSummary {
  empty: boolean;
  weeks: { weekStart: string; distanceM: number }[];
  load: {
    lastTwoWeeksM: number;
    previousTwoWeeksM: number;
    changePercent: number | null;
    warning: boolean;
    hint: string | null;
  };
  longestRun: RunRecord | null;
  bestPace: RunRecord | null;
  effort: { easy: number; moderate: number; hard: number; unrated: number; hint: string | null };
}

/** Итоги месяца и полный отчёт (analytics: MonthlyReportResponse) */
export interface MetricChange {
  before: number | null;
  after: number | null;
  /** null — сравнивать не с чем (в прошлом месяце не было данных) */
  changePercent: number | null;
  hint: string;
}

export interface MonthlyReport {
  month: string; // "2026-10"
  /** в этом месяце тренировок не было */
  empty: boolean;
  /** null — нет ни одного упражнения с расчётным максимумом */
  strength: MetricChange | null;
  tonnage: MetricChange;
  workouts: { count: number; previousCount: number; changePercent: number | null; hint: string };
  recordsCount: number;
  exercises: {
    exerciseId: string;
    exerciseName: string;
    beforeE1rmKg: number | null;
    afterE1rmKg: number | null;
    changePercent: number | null;
    sessions: number;
  }[];
}

export const analyticsApi = {
  monthly: (month: string) => api<MonthlyReport>(`/api/reports/monthly?month=${month}`),
  dashboard: () => api<Dashboard>('/api/analytics/dashboard'),
  progress: (exerciseId: string, period: ProgressPeriod = 'M3') =>
    api<ExerciseProgress>(`/api/analytics/exercises/${exerciseId}/progress?period=${period}`),
  insights: (exerciseId: string) => api<ExerciseInsights>(`/api/analytics/exercises/${exerciseId}/insights`),
  records: (limit = 50) => api<PersonalRecord[]>(`/api/records?limit=${limit}`),
  running: () => api<RunningSummary>('/api/analytics/running'),
  deloadPlan: (exerciseId: string) => api<DeloadPlan>(`/api/analytics/exercises/${exerciseId}/deload-plan`),
};
