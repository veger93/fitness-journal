import { useQueries, useQuery } from '@tanstack/react-query';
import { authApi } from '../api/auth';
import { trainerApi, type Client, type ClientSignal } from '../api/trainer';
import type { Workout } from '../api/workouts';

export const CLIENTS_KEY = ['trainer', 'clients'] as const;

/** Я тренер? (флаг в /api/users/me) */
export function useIsTrainer(): boolean {
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  return me.data?.trainer ?? false;
}

/** Мои клиенты — грузим только тренеру */
export function useClients() {
  const isTrainer = useIsTrainer();
  return useQuery({ queryKey: CLIENTS_KEY, queryFn: trainerApi.clients, enabled: isTrainer });
}

/** Раз в минуту: тренировки клиентов начинаются и заканчиваются не чаще */
const REFRESH_MS = 60_000;

/**
 * У кого из клиентов есть незавершённая тренировка (идёт или подготовлена): clientId -> тренировка.
 * useQueries — по запросу на клиента (у каждого свой X-Client-Id); клиентов у тренера десятки, это нормально.
 */
export function useClientsInProgress(clients: Client[]): Map<string, Workout> {
  const results = useQueries({
    queries: clients.map((client) => ({
      queryKey: ['trainer', 'client-current', client.id],
      queryFn: () => trainerApi.clientCurrentWorkout(client.id),
      refetchInterval: REFRESH_MS,
    })),
  });
  const map = new Map<string, Workout>();
  results.forEach((result, i) => {
    if (result.data) {
      map.set(clients[i].id, result.data);
    }
  });
  return map;
}

/** Сигналы по клиентам (перегрузка, плато, давно не был, рекорд): clientId -> сигнал. Обновляем раз в минуту */
export function useClientSignals(): Map<string, ClientSignal> {
  const isTrainer = useIsTrainer();
  const signals = useQuery({
    queryKey: ['trainer', 'signals'],
    queryFn: trainerApi.clientSignals,
    enabled: isTrainer,
    refetchInterval: REFRESH_MS,
  });
  return new Map((signals.data ?? []).map((signal) => [signal.clientId, signal]));
}
