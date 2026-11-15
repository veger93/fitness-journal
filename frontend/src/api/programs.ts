import { api } from './client';
import type { ExperienceLevel } from './users';
import type { DayOfWeek, Equipment } from './workouts';

/** Типы повторяют DTO workout-service: ProgramRequest, ProgramResponse (docs/program.md, раздел 8). */
export type ProgramGoal = 'MUSCLE' | 'STRENGTH' | 'FAT_LOSS';
export type ProgramLocation = 'GYM' | 'HOME_DUMBBELLS';
export type BodyLimit = 'KNEES' | 'LOWER_BACK' | 'SHOULDERS';
export type PriorityGroup = 'CHEST' | 'BACK' | 'LEGS' | 'ARMS' | 'SHOULDERS';

/** Ответы анкеты. Опыт не спрашиваем — берём из профиля (он уже есть после онбординга) */
export interface ProgramAnswers {
  goal: ProgramGoal;
  daysPerWeek: number;
  sessionMinutes: number;
  location: ProgramLocation;
  experienceLevel: ExperienceLevel;
  limits: BodyLimit[];
  /** null — «всё ровно» */
  priority: PriorityGroup | null;
}

export interface ProgramSlot {
  id: string;
  position: number;
  exerciseId: string;
  exerciseName: string;
  equipment: Equipment;
  imageKey: string | null;
  movementPattern: string;
  sets: number;
  repsMin: number;
  repsMax: number;
  restSec: number;
  swapped: boolean;
  /** что стояло до замены */
  originalExerciseName: string | null;
}

export interface ProgramDay {
  /** день программы — обычный комплекс («Мои комплексы») */
  templateId: string;
  name: string;
  /** в какие дни стоит в плане недели; пусто — человек убрал его из плана */
  weekDays: DayOfWeek[];
  slots: ProgramSlot[];
}

export interface Program extends ProgramAnswers {
  id: string;
  title: string;
  startedAt: string;
  /** "2026-12-26": дата пересмотра (6 недель) */
  reviewAt: string;
  reviewDue: boolean;
  /** пояснения генератора: что заменили и почему */
  notes: string[];
  days: ProgramDay[];
}

export const programsApi = {
  /**
   * null — программы нет (сервер отвечает 204, тело пустое → undefined).
   * Переводим в null: TanStack Query не принимает undefined как данные запроса
   */
  current: () => api<Program | undefined>('/api/programs/current').then((p) => p ?? null),
  /** собрать по анкете: прежняя уходит в архив, план недели заменяется днями программы */
  create: (answers: ProgramAnswers) => api<Program>('/api/programs', { method: 'POST', body: answers }),
};
