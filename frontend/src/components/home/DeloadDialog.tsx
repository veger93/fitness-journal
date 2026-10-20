import { useQuery } from '@tanstack/react-query';
import { analyticsApi, type ExerciseSummary } from '../../api/analytics';
import { formatKg } from '../../lib/format';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import styles from './Home.module.css';

/** План разгрузочной недели (экран "План разгрузки" из карты — пока модальным окном). */
export function DeloadDialog({ exercise, onClose }: { exercise: ExerciseSummary; onClose: () => void }) {
  const plan = useQuery({
    queryKey: ['deload-plan', exercise.exerciseId],
    queryFn: () => analyticsApi.deloadPlan(exercise.exerciseId),
  });

  return (
    <Dialog open onClose={onClose} title="План разгрузки" subtitle={exercise.exerciseName}>
      {plan.isPending && <p className={styles.muted}>Составляем план…</p>}
      {plan.isError && <p className={styles.muted}>{plan.error.message}</p>}
      {plan.data && (
        <>
          <p className={styles.signalText}>{plan.data.reason}</p>
          <div className={styles.planGrid}>
            <div className={styles.planCell}>
              <span className={styles.planValue}>{plan.data.durationDays} дн.</span>
              <span className={styles.muted}>длительность</span>
            </div>
            <div className={styles.planCell}>
              <span className={styles.planValue}>
                {plan.data.workingWeightKg != null ? formatKg(plan.data.workingWeightKg) : '—'}
              </span>
              <span className={styles.muted}>рабочий вес*</span>
            </div>
            <div className={styles.planCell}>
              <span className={styles.planValue}>
                {plan.data.sets} × {plan.data.reps}
              </span>
              <span className={styles.muted}>подходы × повторы</span>
            </div>
          </div>
          <ul className={styles.list}>
            {plan.data.notes.map((note) => (
              <li key={note}>{note}</li>
            ))}
          </ul>
          <p className="footnote">
            * {plan.data.intensityPercent}% от вашего расчётного максимума — веса, который вы подняли бы один раз.
          </p>
        </>
      )}
      <Button fullWidth onClick={onClose}>
        Понятно
      </Button>
    </Dialog>
  );
}
