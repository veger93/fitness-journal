import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { authApi, type AuthResponse } from '../api/auth';
import { configureApiClient, refreshAccessToken } from '../api/client';

interface AuthContextValue {
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Старый вариант хранил токен в localStorage — подчищаем, чтобы он не валялся в браузере */
const LEGACY_SESSION_KEY = 'vegas.session';

/**
 * Вход и токены.
 * Access-токен — только в памяти (useRef): XSS-скрипт не найдёт его в localStorage, а после перезагрузки
 * страницы мы получаем новый по refresh-cookie (httpOnly, JavaScript её не видит).
 * Пока идёт первая проверка "есть ли живой вход" — ничего не рисуем, иначе мелькнул бы экран входа.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  // useRef, а не только useState: клиент API читает токен синхронно и всегда должен видеть последний
  const tokenRef = useRef<string | null>(null);
  const [isAuthenticated, setAuthenticated] = useState(false);
  const [checking, setChecking] = useState(true);

  const setToken = useCallback((token: string | null) => {
    tokenRef.current = token;
    setAuthenticated(token !== null);
  }, []);

  /** Сменился пользователь (вошли, вышли) — кэш с чужими данными выбрасываем. При простом обновлении токена — нет */
  const startSession = useCallback(
    (token: string | null) => {
      setToken(token);
      queryClient.clear();
    },
    [queryClient, setToken],
  );

  configureApiClient({
    getToken: () => tokenRef.current,
    onTokenRefreshed: setToken,
    onUnauthorized: () => startSession(null),
  });

  useEffect(() => {
    try {
      localStorage.removeItem(LEGACY_SESSION_KEY);
    } catch {
      // хранилище недоступно — и чистить нечего
    }
    // при открытии страницы: есть живая refresh-cookie — молча входим, нет — покажем экран входа
    let cancelled = false;
    refreshAccessToken().then((token) => {
      if (!cancelled) {
        setToken(token);
        setChecking(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [setToken]);

  const fromResponse = (response: AuthResponse) => startSession(response.accessToken);

  const value: AuthContextValue = {
    isAuthenticated,
    login: async (email, password) => fromResponse(await authApi.login(email, password)),
    register: async (email, password, displayName) =>
      fromResponse(await authApi.register(email, password, displayName)),
    logout: async () => {
      try {
        await authApi.logout();
      } catch {
        // сервер недоступен — всё равно выходим локально; refresh-токен истечёт сам
      }
      startSession(null);
    },
  };

  if (checking) {
    return null;
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth() вызван вне <AuthProvider>');
  }
  return value;
}
