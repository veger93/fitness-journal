import { api } from './client';

/** Совпадает с user-service: dto/AuthResponse */
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number; // секунд
  userId: string;
}

/** user-service: dto/UserResponse */
export interface UserResponse {
  id: string;
  /** null у клиента, которого ведёт тренер (входа нет) */
  email: string | null;
  displayName: string | null;
  authProvider: string;
  role: string;
  /** включён режим тренера — в шапке появляется переключатель "Я / клиенты" */
  trainer: boolean;
  createdAt: string;
}

/** user-service: dto/ClaimPreviewResponse — что передают по ссылке */
export interface ClaimPreview {
  clientName: string | null;
  trainerName: string | null;
  expiresAt: string;
}

/** Клиент принимает аккаунт от тренера */
export interface ClaimBody {
  email: string;
  password: string;
  /** true — продолжаю с тренером, false — дальше сам */
  keepTrainer: boolean;
  consent: boolean;
}

export const authApi = {
  login: (email: string, password: string) =>
    api<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, password } }),

  register: (email: string, password: string, displayName: string) =>
    api<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: { email, password, displayName: displayName.trim() || null },
    }),

  /** Отозвать refresh-токен и удалить cookie. Access-токен фронт просто забывает */
  logout: () => api<void>('/api/auth/logout', { method: 'POST' }),

  me: () => api<UserResponse>('/api/users/me'),

  claimPreview: (token: string) => api<ClaimPreview>(`/api/auth/claim/${encodeURIComponent(token)}`),
  claim: (token: string, body: ClaimBody) =>
    api<AuthResponse>(`/api/auth/claim/${encodeURIComponent(token)}`, { method: 'POST', body }),
};
