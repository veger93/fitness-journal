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

/** user-service: dto/ClaimLinkResponse — токен одноразовой ссылки передачи аккаунта */
export interface ClaimLink {
  token: string;
  expiresAt: string;
}

/** user-service: dto/MyTrainerResponse */
export interface MyTrainer {
  trainerId: string;
  trainerName: string | null;
  since: string;
}

const base = '/api/users/me';

export const trainerApi = {
  setTrainerMode: (enabled: boolean) => api<UserResponse>(`${base}/trainer-mode`, { method: 'PUT', body: { enabled } }),
  clients: () => api<Client[]>(`${base}/clients`),
  client: (clientId: string) => api<Client>(`${base}/clients/${clientId}`),
  createClient: (body: NewClient) => api<Client>(`${base}/clients`, { method: 'POST', body }),
  updateClient: (clientId: string, body: { displayName: string; phone: string | null }) =>
    api<Client>(`${base}/clients/${clientId}`, { method: 'PUT', body }),
  /** Убрать клиента: без аккаунта — удалить вместе с данными, со своим аккаунтом — отвязать */
  removeClient: (clientId: string) => api<void>(`${base}/clients/${clientId}`, { method: 'DELETE' }),
  /** "Передать аккаунт": новая ссылка на 7 дней, старые перестают работать */
  createClaimLink: (clientId: string) => api<ClaimLink>(`${base}/clients/${clientId}/claim-link`, { method: 'POST' }),
  /** со стороны клиента: мой тренер (null — тренера нет) */
  myTrainer: async () => (await api<MyTrainer | undefined>(`${base}/trainer`)) ?? null,
  leaveTrainer: () => api<void>(`${base}/trainer`, { method: 'DELETE' }),
  /** Идёт ли у клиента тренировка — независимо от того, кто сейчас выбран в переключателе */
  clientCurrentWorkout: async (clientId: string) =>
    (await api<Workout | undefined>('/api/workouts/current', { asClient: clientId })) ?? null,
};
