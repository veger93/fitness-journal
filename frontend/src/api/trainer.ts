import type { UserResponse } from './auth';
import { api } from './client';
import type { ExperienceLevel } from './users';
import type { Workout } from './workouts';

/** user-service: dto/ClientResponse */
export interface Client {
  id: string;
  displayName: string;
  phone: string | null;
  /** true — клиент без входа, дневник ведёт тренер; false — у клиента свой аккаунт */
  managed: boolean;
  heightCm: number | null;
  experienceLevel: ExperienceLevel | null;
  currentWeightKg: number | null;
  linkedAt: string;
}

/** Обязательно только имя; остальное — null, если не указано */
export interface NewClient {
  displayName: string;
  phone: string | null;
  heightCm: number | null;
  experienceLevel: ExperienceLevel | null;
  weightKg: number | null;
}

const base = '/api/users/me';

export const trainerApi = {
  setTrainerMode: (enabled: boolean) => api<UserResponse>(`${base}/trainer-mode`, { method: 'PUT', body: { enabled } }),
  clients: () => api<Client[]>(`${base}/clients`),
  client: (clientId: string) => api<Client>(`${base}/clients/${clientId}`),
  createClient: (body: NewClient) => api<Client>(`${base}/clients`, { method: 'POST', body }),
  updateClient: (clientId: string, body: { displayName: string; phone: string | null }) =>
    api<Client>(`${base}/clients/${clientId}`, { method: 'PUT', body }),
  /** Идёт ли у клиента тренировка — независимо от того, кто сейчас выбран в переключателе */
  clientCurrentWorkout: async (clientId: string) =>
    (await api<Workout | undefined>('/api/workouts/current', { asClient: clientId })) ?? null,
};
