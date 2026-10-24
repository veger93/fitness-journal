import { api } from './client';

/** Типы повторяют DTO workout-service. */
export type TrackingType = 'WEIGHT_REPS' | 'TIME' | 'DISTANCE';
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
  sets: WorkoutSet[];
  previousSets: WorkoutSet[]; // как было в прошлый раз
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
  trackingType: TrackingType;
}

export interface WorkoutTemplate {
  id: string;
  name: string;
  color: string | null;
  icon: string | null;
  custom: boolean;
  exercises: TemplateExercise[];
}

export interface CatalogExercise {
  id: string;
  name: string;
  bodyPart: BodyPart;
  trackingType: TrackingType;
  custom: boolean;
}

/** Строка истории (workout-service: WorkoutSummaryResponse) */
export interface WorkoutHistoryItem {
  id: string;
  name: string;
  startedAt: string;
  completedAt: string;
  exerciseCount: number;
  workingSets: number;
  tonnageKg: number;
  distanceM: number;
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
  history: (page: number, size = 20) => api<Page<WorkoutHistoryItem>>(`${base}?page=${page}&size=${size}`),
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
  deleteTemplate: (id: string) => api<void>(`/api/templates/${id}`, { method: 'DELETE' }),
  exercises: (search: string) => api<CatalogExercise[]>(`/api/exercises?search=${encodeURIComponent(search)}`),
};
