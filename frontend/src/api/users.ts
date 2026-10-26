import { api } from './client';

/** Типы повторяют DTO user-service (ProfileResponse, UpdateProfileRequest, WeightEntryResponse). */
export type Gender = 'MALE' | 'FEMALE';
export type ExperienceLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface Profile {
  gender: Gender | null;
  birthYear: number | null;
  heightCm: number | null;
  experienceLevel: ExperienceLevel | null;
  /** false — пользователь ещё не прошёл онбординг: показываем экран "Расскажите о себе" */
  onboardingCompleted: boolean;
  currentWeightKg: number | null;
}

/** PUT = полная замена: всегда отправляем все поля */
export interface UpdateProfile {
  gender: Gender;
  birthYear: number;
  heightCm: number;
  experienceLevel: ExperienceLevel;
  weightKg: number;
}

export interface WeightEntry {
  id: string;
  weightKg: number;
  /** "2026-10-27" — день по календарю пользователя */
  measuredOn: string;
}

export const usersApi = {
  profile: () => api<Profile>('/api/users/me/profile'),
  updateProfile: (body: UpdateProfile) => api<Profile>('/api/users/me/profile', { method: 'PUT', body }),
  weightHistory: () => api<WeightEntry[]>('/api/users/me/weight'),
  /** PUT, а не POST: повторная отправка за тот же день просто перезапишет вес (идемпотентно) */
  recordWeight: (date: string, weightKg: number) =>
    api<WeightEntry>(`/api/users/me/weight/${date}`, { method: 'PUT', body: { weightKg } }),
};

/**
 * Сегодняшняя дата по часам пользователя: "2026-10-27".
 * Не toISOString(): он переводит время в UTC, и поздно вечером в Москве получилось бы уже "завтра" или "вчера".
 */
export function localDate(date: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
