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
  /** email подтверждён кодом из письма */
  emailVerified: boolean;
  displayName: string | null;
  authProvider: string;
  role: string;
  /** включён режим тренера — в шапке появляется переключатель "Я / клиенты" */
  trainer: boolean;
  createdAt: string;
  /** демо "посмотреть изнутри" (docs/demo.md): полоса "ДЕМО", без почты и удаления аккаунта */
  demo: boolean;
  /** когда демо удалится; null у настоящих пользователей */
  demoExpiresAt: string | null;
}

/** Кем смотреть демо: спортсмен или тренер */
export type DemoScenario = 'ATHLETE' | 'COACH';

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

  /** Демо: временный пользователь с заполненным дневником, сразу вход (refresh-cookie, как при регистрации) */
  demo: (scenario: DemoScenario) => api<AuthResponse>('/api/auth/demo', { method: 'POST', body: { scenario } }),

  /** Отозвать refresh-токен и удалить cookie. Access-токен фронт просто забывает */
  logout: () => api<void>('/api/auth/logout', { method: 'POST' }),

  me: () => api<UserResponse>('/api/users/me'),

  /** "Забыли пароль?": код уходит, только если email подтверждён; ответ всегда одинаковый */
  forgotPassword: (email: string) => api<void>('/api/auth/password/forgot', { method: 'POST', body: { email } }),
  resetPassword: (body: { email: string; code: string; newPassword: string }) =>
    api<void>('/api/auth/password/reset', { method: 'POST', body }),

  /** подтверждение и смена своего email */
  resendEmailCode: () => api<void>('/api/users/me/email/verification', { method: 'POST' }),
  confirmEmail: (code: string) =>
    api<UserResponse>('/api/users/me/email/verification/confirm', { method: 'POST', body: { code } }),
  changeEmail: (email: string, password: string) =>
    api<UserResponse>('/api/users/me/email', { method: 'PUT', body: { email, password } }),
  /** удалить свой аккаунт со всеми данными (docs/account.md, шаг 2); тренер — вместе с клиентами без аккаунта */
  deleteAccount: (password: string) => api<void>('/api/users/me/deletion', { method: 'POST', body: { password } }),

  claimPreview: (token: string) => api<ClaimPreview>(`/api/auth/claim/${encodeURIComponent(token)}`),
  claim: (token: string, body: ClaimBody) =>
    api<AuthResponse>(`/api/auth/claim/${encodeURIComponent(token)}`, { method: 'POST', body }),
};
