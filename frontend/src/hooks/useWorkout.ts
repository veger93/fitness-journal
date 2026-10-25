import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { workoutsApi, type SetInput, type Workout } from '../api/workouts';
import { withSetAdded, withSetDeleted, withSetUpdated } from '../lib/workout';

export const CURRENT_WORKOUT_KEY = ['workout', 'current'] as const;

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

  const addSet = useMutation({
    mutationFn: ({ exerciseId, set }: { exerciseId: string; set: SetInput }) =>
      workoutsApi.addSet(workout.id, exerciseId, set),
    onSuccess: (saved, { exerciseId }) => {
      update((w) => withSetAdded(w, exerciseId, saved));
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
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['history'] });
    queryClient.invalidateQueries({ queryKey: ['running'] });
  };

  const complete = useMutation({
    mutationFn: () => workoutsApi.complete(workout.id),
    onSuccess: (done) => finish(`/workout/${done.id}/summary`),
  });
  const cancel = useMutation({ mutationFn: () => workoutsApi.remove(workout.id), onSuccess: () => finish('/') });

  return { begin, addSet, updateSet, deleteSet, addExercise, removeExercise, complete, cancel };
}
