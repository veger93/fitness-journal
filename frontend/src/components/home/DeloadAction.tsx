import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { DeloadPlan } from '../../api/analytics';
import { workoutsApi } from '../../api/workouts';
import { CURRENT_WORKOUT_KEY } from '../../hooks/useWorkout';
import { formatKg, formatShortDate } from '../../lib/format';
import { Button } from '../ui/Button';
import styles from './Home.module.css';

/**
 * "Применить план": включить режим разгрузки по упражнению. План на неделю не меняется —
 * просто в каждом подходе этого упражнения подсказка предложит облегчённый вес и схему, пока идёт разгрузка.
 */
export function DeloadAction({ plan }: { plan: DeloadPlan }) {
  const queryClient = useQueryClient();
  const deloads = useQuery({ queryKey: ['deloads'], queryFn: workoutsApi.deloads });
  const active = deloads.data?.find((d) => d.exerciseId === plan.exerciseId) ?? null;

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['deloads'] });
    // в идущей тренировке подсказка подхода должна сразу учесть разгрузку
    queryClient.invalidateQueries({ queryKey: CURRENT_WORKOUT_KEY });
  };
  const start = useMutation({
    mutationFn: () =>
      workoutsApi.startDeload(plan.exerciseId, {
        workingWeightKg: plan.workingWeightKg,
        sets: plan.sets,
        reps: plan.reps,
        days: plan.durationDays,
      }),
    onSuccess: refresh,
  });
  const stop = useMutation({ mutationFn: () => workoutsApi.stopDeload(plan.exerciseId), onSuccess: refresh });

  if (deloads.isPending) {
    return null;
  }
  if (active) {
    return (
      <div className={styles.deloadActive}>
        <p className={styles.signalText}>
          <strong>Разгрузка — до {formatShortDate(active.endsAt)}</strong> В тренировке подход подскажет{' '}
          {active.workingWeightKg != null
            ? `${formatKg(active.workingWeightKg)} × ${active.reps}`
            : `${active.reps} повт.`}
          , {active.sets} подхода. План на неделю не меняется.
        </p>
        <button type="button" className={styles.textButton} disabled={stop.isPending} onClick={() => stop.mutate()}>
          Закончить раньше
        </button>
      </div>
    );
  }
  return (
    <>
      <Button disabled={start.isPending} onClick={() => start.mutate()}>
        {start.isPending ? 'Включаем…' : `Разгрузиться ${plan.durationDays} дн.`}
      </Button>
      {start.isError && <p className={styles.muted}>{start.error.message}</p>}
      <p className="footnote">
        План на неделю останется как есть: изменятся только подсказки веса в этом упражнении. Через {plan.durationDays}{' '}
        дн. всё вернётся само.
      </p>
    </>
  );
}
