import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { authApi, type AuthResponse } from '../api/auth';
import { configureApiClient } from '../api/client';
import { loadSession, saveSession, type Session } from './session';

interface AuthContextValue {
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function toSession(response: AuthResponse): Session {
  return { token: response.accessToken, expiresAt: Date.now() + response.expiresIn * 1000 };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => loadSession());

  const apply = useCallback((next: Session | null) => {
    saveSession(next);
    setSession(next);
  }, []);

  // Настраиваем клиент прямо при рендере: запросы дочерних компонентов
  // должны уже видеть актуальный токен.
  configureApiClient({
    getToken: () => session?.token ?? null,
    onUnauthorized: () => apply(null),
  });

  const value: AuthContextValue = {
    isAuthenticated: session !== null,
    login: async (email, password) => apply(toSession(await authApi.login(email, password))),
    register: async (email, password, displayName) =>
      apply(toSession(await authApi.register(email, password, displayName))),
    logout: () => apply(null),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth() вызван вне <AuthProvider>');
  }
  return value;
}
