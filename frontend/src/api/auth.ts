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
  email: string;
  displayName: string | null;
  authProvider: string;
  role: string;
  createdAt: string;
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
};
