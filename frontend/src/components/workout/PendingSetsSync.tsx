import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { authApi } from '../../api/auth';
import { getActingClient } from '../../api/client';
import type { WorkoutSet } from '../../api/workouts';
import { ADD_SET_KEY, useCurrentWorkout } from '../../hooks/useWorkout';
import { setPendingOwner, updatePendingSets, usePendingSetsStore, type PendingSet } from '../../lib/pendingSets';

/**
 * Невидимый помощник (стоит в AppLayout, то есть на всех страницах после входа):
 * 1) при запуске приложения достаёт из памяти телефона подходы, которые не успели сохраниться
 *    (iPhone выгрузил приложение, пока шёл запрос), и досылает их. Повтор безопасен: сервер узнаёт clientId;
 * 2) убирает из очереди не сохранившиеся подходы ЗАВЕРШЁННОЙ или отменённой тренировки:
 *    их уже некуда записать, и иначе точка «не сохранено» висела бы вечно.
 */
export function PendingSetsSync() {
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const send = useMutation<WorkoutSet, Error, PendingSet>({ mutationKey: ADD_SET_KEY });
  const { mutate } = send;
  const userId = me.data?.id ?? null;

  useEffect(() => {
    if (userId) {
      setPendingOwner(userId).forEach((pending) => mutate(pending));
    }
  }, [userId, mutate]);

  const current = useCurrentWorkout();
  const pending = usePendingSetsStore();
  useEffect(() => {
    if (!current.isSuccess) {
      return;
    }
    const acting = getActingClient();
    const currentId = current.data?.id ?? null;
    const stale = pending.filter((p) => p.status === 'failed' && p.asClient === acting && p.workoutId !== currentId);
    if (stale.length > 0) {
      updatePendingSets((list) => list.filter((p) => !stale.includes(p)));
    }
  }, [current.isSuccess, current.data, pending]);

  return null;
}
