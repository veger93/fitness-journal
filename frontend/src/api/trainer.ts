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

/** Главный сигнал по клиенту (analytics: ClientSignalResponse) — метка в списке "Чей дневник открыть" */
export interface ClientSignal {
  clientId: string;
  signal: 'OVERLOAD' | 'PLATEAU' | 'AWAY' | 'RECORD' | 'NONE';
  /** в каком упражнении (у AWAY и NONE — null) */
  exerciseName: string | null;
  /** PLATEAU — дней без максимума, AWAY — без тренировок, RECORD — с рекорда */
  days: number | null;
  lastWorkoutAt: string | null;
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
  /**
   * Сигналы по всем моим клиентам одним запросом. asClient: null — всегда от имени тренера,
   * даже когда сейчас открыт дневник клиента (иначе ушёл бы X-Client-Id и вернулись бы "клиенты клиента")
   */
  clientSignals: () => api<ClientSignal[]>('/api/analytics/trainer/clients', { asClient: null }),
  clientCurrentWorkout: async (clientId: string) =>
    (await api<Workout | undefined>('/api/workouts/current', { asClient: clientId })) ?? null,
};
