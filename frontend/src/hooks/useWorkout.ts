import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ApiError, NO_RESPONSE } from '../api/client';
import { workoutsApi, type SetInput, type Workout } from '../api/workouts';
import { withSetAdded, withSetDeleted, withSetUpdated } from '../lib/workout';

export const CURRENT_WORKOUT_KEY = ['workout', 'current'] as const;

/**
 * Подход, который уже показан в списке, но сервер его ещё не подтвердил.
 * clientId — id, придуманный телефоном: с ним повтор запроса не создаст дубль (сервер узнает подход).
 */
export interface PendingSet {
  clientId: string;
  workoutId: string;
  workoutExerciseId: string;
  input: SetInput;
  /** saving — отправляем (в том числе повторно); failed — все попытки не удались, ждём «повторить» */
  status: 'saving' | 'failed';
}

/**
 * Неподтверждённые подходы живут в кэше TanStack Query, а не в useState страницы:
 * ушли на другую вкладку и вернулись — подходы на месте, запросы в фоне продолжаются.
 */
const PENDING_SETS_KEY = ['workout', 'pendingSets'] as const;

export function usePendingSets(workoutId: string): PendingSet[] {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: PENDING_SETS_KEY,
    queryFn: () => queryClient.getQueryData<PendingSet[]>(PENDING_SETS_KEY) ?? [],
    initialData: [],
    staleTime: Infinity,
    gcTime: Infinity,
  });
  return query.data.filter((p) => p.workoutId === workoutId);
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

  const setPending = (change: (list: PendingSet[]) => PendingSet[]) =>
    queryClient.setQueryData<PendingSet[]>(PENDING_SETS_KEY, (old) => change(old ?? []));

  /**
   * Новый подход — «оптимистично»: сразу в списке с пометкой «сохраняется…», окно закрывается без ожидания.
   * Запрос ждёт ответа 10 с; нет ответа — пробуем ещё 3 раза (через 1, 2, 4 с) с тем же clientId,
   * поэтому даже если первый запрос всё-таки дошёл, второго подхода на сервере не будет.
   * Нет сети вовсе — TanStack Query ставит запрос на паузу и отправит, когда сеть появится.
   */
  const addSet = useMutation({
    mutationFn: ({ exerciseId, set, clientId }: { exerciseId: string; set: SetInput; clientId: string }) =>
      workoutsApi.addSet(workout.id, exerciseId, set, clientId),
    retry: (failureCount, error) => failureCount < 3 && isRetryable(error),
    retryDelay: (attempt) => 1000 * 2 ** attempt,
    onMutate: ({ exerciseId, set, clientId }) =>
      setPending((list) => [
        ...list.filter((p) => p.clientId !== clientId),
        { clientId, workoutId: workout.id, workoutExerciseId: exerciseId, input: set, status: 'saving' },
      ]),
    onError: (_, { clientId }) =>
      setPending((list) => list.map((p) => (p.clientId === clientId ? { ...p, status: 'failed' } : p))),
    onSuccess: (saved, { exerciseId, clientId }) => {
      // сначала — в тренировку, потом — из «неподтверждённых»: подход ни на миг не пропадает с экрана
      update((w) => withSetAdded(w, exerciseId, saved));
      setPending((list) => list.filter((p) => p.clientId !== clientId));
      // первый подход сам начинает тренировку на сервере — перечитаем, чтобы получить время начала
      if (!workout.startedAt) {
        queryClient.invalidateQueries({ queryKey: CURRENT_WORKOUT_KEY });
      }
    },
  });

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
    setPending((list) => list.filter((p) => p.workoutId !== workout.id));
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

  /** «Не сохранён — повторить»: тот же подход, тот же clientId */
  const retrySet = (pending: PendingSet) =>
    addSet.mutate({ exerciseId: pending.workoutExerciseId, set: pending.input, clientId: pending.clientId });

  return {
    begin,
    addSet,
    retrySet,
    updateSet,
    deleteSet,
    addExercise,
    removeExercise,
    complete,
    cancel,
  };
}
