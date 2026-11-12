import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ApiError, NO_RESPONSE, getActingClient } from '../api/client';
import { workoutsApi, type SetInput, type Workout, type WorkoutSet } from '../api/workouts';
import { updatePendingSets, usePendingSetsStore, type PendingSet } from '../lib/pendingSets';
import { withSetAdded, withSetDeleted, withSetUpdated } from '../lib/workout';

export type { PendingSet } from '../lib/pendingSets';

export const CURRENT_WORKOUT_KEY = ['workout', 'current'] as const;

export const ADD_SET_KEY = ['workout', 'addSet'] as const;

/** Неподтверждённые подходы одной тренировки (очередь — lib/pendingSets) */
export function usePendingSets(workoutId: string): PendingSet[] {
  return usePendingSetsStore().filter((p) => p.workoutId === workoutId);
}

/** Сколько подходов не сохранилось в текущем дневнике (свой или клиента) — для точки на вкладке и строки на главной */
export function useFailedSetsCount(): number {
  const acting = getActingClient();
  return usePendingSetsStore().filter((p) => p.status === 'failed' && p.asClient === acting).length;
}

/**
 * Запись подхода описана ОДИН раз, на весь QueryClient (setMutationDefaults), а не внутри страницы:
 * её запускают и экран тренировки, и PendingSetsSync после перезапуска приложения. Колбэки здесь
 * срабатывают, даже если экран тренировки уже закрыт.
 *
 * Новый подход — «оптимистично»: сразу в очереди и на экране («сохраняется…»), окно закрывается без ожидания.
 * Запрос ждёт ответа 10 с; нет ответа — ещё 3 попытки (через 1, 2, 4 с) с тем же clientId.
 * Нет сети вовсе — TanStack Query ставит запрос на паузу и отправит, когда сеть появится.
 */
export function registerAddSetMutation(queryClient: QueryClient) {
  queryClient.setMutationDefaults(ADD_SET_KEY, {
    mutationFn: (p: PendingSet) =>
      workoutsApi.addSet(p.workoutId, p.workoutExerciseId, p.input, p.clientId, p.asClient),
    retry: (failureCount, error) => failureCount < 3 && isRetryable(error),
    retryDelay: (attempt) => 1000 * 2 ** attempt,
    onMutate: (p: PendingSet) =>
      updatePendingSets((list) => [...list.filter((x) => x.clientId !== p.clientId), { ...p, status: 'saving' }]),
    onError: (_error: unknown, p: PendingSet) =>
      updatePendingSets((list) => list.map((x) => (x.clientId === p.clientId ? { ...x, status: 'failed' } : x))),
    onSuccess: (saved: WorkoutSet, p: PendingSet) => {
      // сначала — в тренировку на экране, потом — из очереди: подход ни на миг не пропадает
      const current = queryClient.getQueryData<Workout | null>(CURRENT_WORKOUT_KEY);
      if (current?.id === p.workoutId && getActingClient() === p.asClient) {
        queryClient.setQueryData<Workout | null>(
          CURRENT_WORKOUT_KEY,
          withSetAdded(current, p.workoutExerciseId, saved),
        );
        // первый подход сам начинает тренировку на сервере — перечитаем, чтобы получить время начала
        if (!current.startedAt) {
          queryClient.invalidateQueries({ queryKey: CURRENT_WORKOUT_KEY });
        }
      }
      updatePendingSets((list) => list.filter((x) => x.clientId !== p.clientId));
    },
  });
}

/**
 * Повторяем только когда ответа не было (нет сети, таймаут), сервер был недоступен (5xx)
 * или два одинаковых запроса столкнулись (409). Ошибку в данных (400) повтор не исправит.
 */
export function isRetryable(error: unknown): boolean {
  if (!(error instanceof ApiError)) {
    return false;
  }
  return error.status === NO_RESPONSE || error.status === 409 || error.status >= 500;
}

/** id подхода на телефоне. crypto.randomUUID есть во всех свежих браузерах; запасной вариант — на всякий случай */
export function newClientId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export function useCurrentWorkout() {
  return useQuery({ queryKey: CURRENT_WORKOUT_KEY, queryFn: workoutsApi.current });
}

/**
 * Все изменения идущей тренировки. После ответа сервера кладём новую версию прямо в кэш
 * (setQueryData), а не перезапрашиваем всю тренировку: в зале интернет бывает плохим,
 * лишний запрос — лишнее ожидание.
 */
export function useWorkoutActions(workout: Workout) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const update = (change: (w: Workout) => Workout) =>
    queryClient.setQueryData<Workout | null>(CURRENT_WORKOUT_KEY, (old) => (old ? change(old) : old));
  const replace = (next: Workout) => queryClient.setQueryData(CURRENT_WORKOUT_KEY, next);

  // запись подхода: всё поведение — в registerAddSetMutation, здесь только «ручка» для вызова
  const addSet = useMutation<WorkoutSet, Error, PendingSet>({ mutationKey: ADD_SET_KEY });

  const begin = useMutation({ mutationFn: () => workoutsApi.begin(workout.id), onSuccess: replace });

  const updateSet = useMutation({
    mutationFn: ({ exerciseId, setId, set }: { exerciseId: string; setId: string; set: SetInput }) =>
      workoutsApi.updateSet(workout.id, exerciseId, setId, set),
    onSuccess: (saved, { exerciseId }) => update((w) => withSetUpdated(w, exerciseId, saved)),
  });

  const deleteSet = useMutation({
    mutationFn: ({ exerciseId, setId }: { exerciseId: string; setId: string }) =>
      workoutsApi.deleteSet(workout.id, exerciseId, setId),
    onSuccess: (_, { exerciseId, setId }) => update((w) => withSetDeleted(w, exerciseId, setId)),
  });

  const addExercise = useMutation({
    mutationFn: (exerciseId: string) => workoutsApi.addExercise(workout.id, exerciseId),
    onSuccess: replace,
  });

  const removeExercise = useMutation({
    mutationFn: (workoutExerciseId: string) => workoutsApi.removeExercise(workout.id, workoutExerciseId),
    onSuccess: replace,
  });

  /**
   * Завершение и отмена: СНАЧАЛА уходим со страницы, потом чистим кэш.
   * Наоборот нельзя: пустой кэш перерисует экран тренировки, он сам уйдёт на /workout,
   * а колбэки mutate() у исчезнувшего компонента TanStack Query уже не вызывает.
   * Поэтому переход здесь, в onSuccess самой мутации — он срабатывает всегда.
   */
  const finish = (path: string) => {
    navigate(path, { replace: true });
    queryClient.setQueryData(CURRENT_WORKOUT_KEY, null);
    // не сохранённые подходы этой тренировки больше не нужны (при завершении человек это подтвердил)
    updatePendingSets((list) => list.filter((p) => p.workoutId !== workout.id));
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['history'] });
    queryClient.invalidateQueries({ queryKey: ['running'] });
    queryClient.invalidateQueries({ queryKey: ['records'] });
  };

  const complete = useMutation({
    mutationFn: () => workoutsApi.complete(workout.id),
    onSuccess: (done) => finish(`/workout/${done.id}/summary`),
  });
  const cancel = useMutation({ mutationFn: () => workoutsApi.remove(workout.id), onSuccess: () => finish('/') });

  /** Новый подход: в очередь и на сервер. clientId — новый, повторы пойдут с ним же */
  const saveNewSet = (workoutExerciseId: string, input: SetInput) =>
    addSet.mutate({
      clientId: newClientId(),
      workoutId: workout.id,
      workoutExerciseId,
      input,
      asClient: getActingClient(),
      status: 'saving',
    });

  /** «Не сохранён — повторить»: тот же подход, тот же clientId */
  const retrySet = (pending: PendingSet) => addSet.mutate(pending);

  return {
    begin,
    saveNewSet,
    retrySet,
    updateSet,
    deleteSet,
    addExercise,
    removeExercise,
    complete,
    cancel,
  };
}
