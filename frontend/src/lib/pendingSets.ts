import { useSyncExternalStore } from 'react';
import type { SetInput } from '../api/workouts';

/**
 * Очередь подходов, которые уже показаны на экране, но сервер их ещё не подтвердил.
 *
 * Живёт вне React и вне кэша TanStack Query, и копия лежит в localStorage:
 * - ушли на другую вкладку — подходы на месте, запросы продолжаются;
 * - iPhone выгрузил приложение из памяти (переключились на музыку) — при следующем запуске
 *   PendingSetsSync достанет очередь и дошлёт подходы. Дублей не будет: у каждого свой clientId,
 *   сервер узнаёт повтор и возвращает уже сохранённый подход.
 * Кэш TanStack Query не подходит: при переключении тренера на клиента он целиком сбрасывается (resetQueries).
 */
export interface PendingSet {
  /** id подхода, придуманный телефоном: по нему сервер узнаёт повтор */
  clientId: string;
  workoutId: string;
  workoutExerciseId: string;
  input: SetInput;
  /** за кого записан подход (тренер за клиента) — повтор после перезапуска должен уйти тому же человеку */
  asClient: string | null;
  /** saving — отправляем (в том числе повторно); failed — все попытки не удались, ждём «повторить» */
  status: 'saving' | 'failed';
}

const STORAGE_KEY = 'vegas.pendingSets.';

let owner: string | null = null;
let items: PendingSet[] = [];
const listeners = new Set<() => void>();

function read(userId: string): PendingSet[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY + userId);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? (parsed as PendingSet[]) : [];
  } catch {
    return []; // приватный режим или испорченная запись — начинаем с пустой очереди
  }
}

function write() {
  if (!owner) {
    return;
  }
  try {
    if (items.length === 0) {
      localStorage.removeItem(STORAGE_KEY + owner);
    } else {
      localStorage.setItem(STORAGE_KEY + owner, JSON.stringify(items));
    }
  } catch {
    // не сохранилось (приватный режим) — очередь живёт хотя бы в памяти
  }
}

/**
 * Чья очередь: у каждого пользователя своя (на одном телефоне могут входить двое).
 * Возвращает подходы, которые остались с прошлого запуска, — их надо дослать.
 */
export function setPendingOwner(userId: string | null): PendingSet[] {
  if (userId === owner) {
    return [];
  }
  owner = userId;
  items = userId ? read(userId) : [];
  // всё, что осталось с прошлого запуска, — снова «отправляем»
  items = items.map((p) => ({ ...p, status: 'saving' }));
  emit();
  return items;
}

export function updatePendingSets(change: (list: PendingSet[]) => PendingSet[]) {
  items = change(items);
  write();
  emit();
}

export function getPendingSets(): PendingSet[] {
  return items;
}

function emit() {
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Очередь для компонентов: перерисовываются при каждом изменении */
export function usePendingSetsStore(): PendingSet[] {
  return useSyncExternalStore(subscribe, getPendingSets);
}
