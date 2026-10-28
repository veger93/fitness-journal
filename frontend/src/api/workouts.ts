import { api } from './client';

/** Типы повторяют DTO workout-service. */
export type TrackingType = 'WEIGHT_REPS' | 'TIME' | 'DISTANCE';
export type Equipment = 'BARBELL' | 'DUMBBELL' | 'MACHINE' | 'BODYWEIGHT';
export type BodyPart = 'CHEST' | 'BACK' | 'LEGS' | 'SHOULDERS' | 'ARMS' | 'CORE';

export interface WorkoutSet {
  id: string;
  number: number;
  weightKg: number | null;
  reps: number | null;
  durationSec: number | null;
  distanceM: number | null;
  rpe: number | null;
  warmup: boolean;
}

/** Тело запроса подхода: какие поля обязательны — зависит от trackingType упражнения. */
export type SetInput = Omit<WorkoutSet, 'id' | 'number'>;

export interface WorkoutExercise {
  id: string; // id упражнения ВНУТРИ тренировки
  exerciseId: string; // id из каталога
  exerciseName: string;
  trackingType: TrackingType;
  equipment: Equipment;
  sets: WorkoutSet[];
  previousSets: WorkoutSet[]; // как было в прошлый раз
  /** активная разгрузка: подсказка подхода берёт вес и повторы отсюда */
  deload: Deload | null;
}

/** Режим разгрузки по упражнению (workout-service: DeloadResponse) */
export interface Deload {
  exerciseId: string;
  workingWeightKg: number | null;
  sets: number;
  reps: number;
  startedAt: string;
  endsAt: string;
}

export interface Workout {
  id: string;
  name: string;
  templateId: string | null;
  status: 'IN_PROGRESS' | 'COMPLETED';
  /** null — тренировка подготовлена, но ещё не начата: таймер не идёт */
  startedAt: string | null;
  completedAt: string | null;
  durationSec: number | null;
  workingSets: number;
  tonnageKg: number;
  exercises: WorkoutExercise[];
}

export interface TemplateExercise {
  id: string;
  name: string;
  bodyPart: BodyPart;
  trackingType: TrackingType;
}

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** План на неделю (workout-service: WeeklyPlanResponse): 7 дней с понедельника, template = null — отдых */
export interface WeeklyPlan {
  days: { dayOfWeek: DayOfWeek; template: WorkoutTemplate | null }[];
}

export interface WorkoutTemplate {
  id: string;
  name: string;
  color: string | null;
  icon: string | null;
  custom: boolean;
  exercises: TemplateExercise[];
}

export type MuscleGroup =
  | 'CHEST'
  | 'FRONT_DELTS'
  | 'SIDE_DELTS'
  | 'REAR_DELTS'
  | 'BICEPS'
  | 'TRICEPS'
  | 'FOREARMS'
  | 'ABS'
  | 'OBLIQUES'
  | 'LATS'
  | 'UPPER_BACK'
  | 'TRAPS'
  | 'LOWER_BACK'
  | 'GLUTES'
  | 'QUADS'
  | 'HAMSTRINGS'
  | 'ADDUCTORS'
  | 'CALVES';

/** PRIMARY — основная работа, SECONDARY — помогают */
export type MuscleRole = 'PRIMARY' | 'SECONDARY';

export interface ExerciseMuscle {
  muscle: MuscleGroup;
  role: MuscleRole;
}

/** GET /api/exercises (workout-service: ExerciseResponse) */
export interface CatalogExercise {
  id: string;
  name: string;
  bodyPart: BodyPart;
  equipment: Equipment;
  trackingType: TrackingType;
  custom: boolean;
  /** у своих упражнений пусто */
  muscles: ExerciseMuscle[];
  /** картинки /exercises/<imageKey>-1.svg и -2.svg; null — картинки нет (свои упражнения, бег, планка) */
  imageKey: string | null;
}

/** GET /api/exercises/{id}/technique */
export interface ExerciseTechnique {
  /** исходное положение */
  setup: string;
  steps: string[];
  mistakes: string[];
}

/** DELETE /api/exercises/{id}: DELETED — удалено совсем, ARCHIVED — есть история, только спрятано из каталога */
export interface DeleteExerciseResult {
  result: 'DELETED' | 'ARCHIVED';
}

/** POST /api/exercises — своё упражнение (workout-service: ExerciseRequest) */
export interface NewExercise {
  name: string;
  bodyPart: BodyPart;
  equipment: Equipment;
  trackingType: TrackingType;
  icon: string | null;
  note: string | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const base = '/api/workouts';
const exercisePath = (workoutId: string, workoutExerciseId: string) =>
  `${base}/${workoutId}/exercises/${workoutExerciseId}`;

export const workoutsApi = {
  /** 204 (тренировки нет) клиент возвращает как undefined — превращаем в null: так понятнее и кэшируется */
  current: async () => (await api<Workout | undefined>(`${base}/current`)) ?? null,
  get: (id: string) => api<Workout>(`${base}/${id}`),
  /** журнал: тренировки целиком (упражнения и подходы), новые сверху */
  /** exerciseId — только тренировки, где было это упражнение */
  journal: (page: number, size = 10, exerciseId?: string) =>
    api<Page<Workout>>(`${base}/journal?page=${page}&size=${size}${exerciseId ? `&exerciseId=${exerciseId}` : ''}`),
  start: (templateId: string | null) => api<Workout>(base, { method: 'POST', body: templateId ? { templateId } : {} }),
  begin: (id: string) => api<Workout>(`${base}/${id}/start`, { method: 'POST' }),
  complete: (id: string) => api<Workout>(`${base}/${id}/complete`, { method: 'POST' }),
  remove: (id: string) => api<void>(`${base}/${id}`, { method: 'DELETE' }),

  addExercise: (workoutId: string, exerciseId: string) =>
    api<Workout>(`${base}/${workoutId}/exercises`, { method: 'POST', body: { exerciseId } }),
  removeExercise: (workoutId: string, workoutExerciseId: string) =>
    api<Workout>(exercisePath(workoutId, workoutExerciseId), { method: 'DELETE' }),

  addSet: (workoutId: string, workoutExerciseId: string, set: SetInput) =>
    api<WorkoutSet>(`${exercisePath(workoutId, workoutExerciseId)}/sets`, { method: 'POST', body: set }),
  updateSet: (workoutId: string, workoutExerciseId: string, setId: string, set: SetInput) =>
    api<WorkoutSet>(`${exercisePath(workoutId, workoutExerciseId)}/sets/${setId}`, { method: 'PUT', body: set }),
  deleteSet: (workoutId: string, workoutExerciseId: string, setId: string) =>
    api<void>(`${exercisePath(workoutId, workoutExerciseId)}/sets/${setId}`, { method: 'DELETE' }),

  templates: () => api<WorkoutTemplate[]>('/api/templates'),
  /** Свой комплекс. color — ключ из палитры бэкенда (orange, blue, green, violet). */
  createTemplate: (name: string, exerciseIds: string[], color = 'blue') =>
    api<WorkoutTemplate>('/api/templates', { method: 'POST', body: { name, color, icon: null, exerciseIds } }),
  plan: () => api<WeeklyPlan>('/api/plan'),
  savePlan: (days: { dayOfWeek: DayOfWeek; templateId: string | null }[]) =>
    api<WeeklyPlan>('/api/plan', { method: 'PUT', body: { days } }),
  deleteTemplate: (id: string) => api<void>(`/api/templates/${id}`, { method: 'DELETE' }),
  deloads: () => api<Deload[]>('/api/deloads'),
  /** PUT: повторный старт заменяет текущую разгрузку, а не создаёт вторую */
  startDeload: (
    exerciseId: string,
    body: { workingWeightKg: number | null; sets: number; reps: number; days: number },
  ) => api<Deload>(`/api/exercises/${exerciseId}/deload`, { method: 'PUT', body }),
  stopDeload: (exerciseId: string) => api<void>(`/api/exercises/${exerciseId}/deload`, { method: 'DELETE' }),
  createExercise: (body: NewExercise) => api<CatalogExercise>('/api/exercises', { method: 'POST', body }),
  deleteExercise: (id: string) => api<DeleteExerciseResult>(`/api/exercises/${id}`, { method: 'DELETE' }),
  exercises: (search: string) => api<CatalogExercise[]>(`/api/exercises?search=${encodeURIComponent(search)}`),
  exercise: (id: string) => api<CatalogExercise>(`/api/exercises/${id}`),
  /** 204 (техники нет) клиент отдаёт как undefined — превращаем в null: TanStack Query не хранит undefined */
  technique: async (id: string) => (await api<ExerciseTechnique | undefined>(`/api/exercises/${id}/technique`)) ?? null,
};
