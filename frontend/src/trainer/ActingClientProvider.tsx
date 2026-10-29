import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { setActingClient } from '../api/client';
import { trainerApi } from '../api/trainer';
import { useAuth } from '../auth/AuthProvider';

/** Клиент, за которого сейчас работает тренер: для полосы сверху хватает id и имени */
export interface ActingClient {
  id: string;
  displayName: string;
}

interface ActingClientValue {
  /** null — тренер в своём дневнике */
  client: ActingClient | null;
  /** переключиться на клиента или (null) вернуться к себе */
  actAs: (client: ActingClient | null) => void;
}

const ActingClientContext = createContext<ActingClientValue | null>(null);

/** sessionStorage: переживает перезагрузку страницы, но не закрытие вкладки — утром тренер начнёт "с себя" */
const STORAGE_KEY = 'vegas.actingClient';
/** Через сколько перезапросить, если связь с новым клиентом ещё в пути (Kafka) */
const RETRY_MS = 1500;

function load(): ActingClient | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as ActingClient) : null;
  } catch {
    return null;
  }
}

function save(client: ActingClient | null) {
  try {
    if (client) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(client));
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // хранилище недоступно — режим просто не переживёт перезагрузку
  }
}

/**
 * Режим тренера (docs/trainer.md, вариант A): чей дневник сейчас открыт.
 *
 * Сам выбор живёт в api/client.ts (setActingClient): клиент API добавляет заголовок X-Client-Id
 * к запросам тренировок и аналитики, и бэкенд отвечает данными клиента. Экраны не меняются вообще.
 *
 * При переключении сбрасываем кэш TanStack Query: ключи запросов не знают, чьи это данные
 * (['dashboard'] у тренера и у клиента — один ключ), поэтому старые ответы нельзя показывать.
 */
export function ActingClientProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuth();
  const [client, setClient] = useState<ActingClient | null>(() => {
    const saved = load();
    // ставим сразу, до первых запросов страницы: иначе она на миг загрузила бы данные тренера
    setActingClient(saved?.id ?? null);
    return saved;
  });

  const actAs = useCallback(
    (picked: ActingClient | null) => {
      // из списка клиентов приходит весь объект — храним только нужное полосе сверху
      const next = picked && { id: picked.id, displayName: picked.displayName };
      setClient(next);
      save(next);
      setActingClient(next?.id ?? null);
      // resetQueries: забыть ответы и заново загрузить то, что сейчас на экране
      queryClient.resetQueries();
    },
    [queryClient],
  );

  /*
   * Сервис ответил 403 "нет доступа к клиенту". Две причины:
   * 1) клиент только что создан, и связь ещё не дошла через Kafka до workout/analytics (это секунда-две) —
   *    тогда клиент есть в списке user-service: подождём и перезапросим;
   * 2) клиент ушёл от тренера — его нет в списке: возвращаемся в свой дневник.
   */
  const checking = useRef(false);
  const onAccessLost = useCallback(async () => {
    if (!client || checking.current) {
      return;
    }
    checking.current = true;
    try {
      const clients = await trainerApi.clients().catch(() => null);
      if (clients?.some((c) => c.id === client.id)) {
        setTimeout(() => queryClient.invalidateQueries(), RETRY_MS);
      } else {
        actAs(null);
      }
    } finally {
      checking.current = false;
    }
  }, [client, actAs, queryClient]);

  // ставим и для клиента, восстановленного из sessionStorage
  useEffect(() => {
    setActingClient(client?.id ?? null, () => void onAccessLost());
  }, [client, onAccessLost]);

  // вышли из аккаунта — следующий вход начинается "с себя"
  useEffect(() => {
    if (!isAuthenticated && client) {
      setClient(null);
      save(null);
      setActingClient(null);
    }
  }, [isAuthenticated, client]);

  return <ActingClientContext.Provider value={{ client, actAs }}>{children}</ActingClientContext.Provider>;
}

export function useActingClient(): ActingClientValue {
  const value = useContext(ActingClientContext);
  if (!value) {
    throw new Error('useActingClient() вызван вне <ActingClientProvider>');
  }
  return value;
}
