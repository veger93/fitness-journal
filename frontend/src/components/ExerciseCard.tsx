import type { ExerciseStatus, ExerciseSummary } from '../api/analytics';
import { formatSet, formatShortDate } from '../lib/format';
import { Badge } from './ui/Badge';
import { Card } from './ui/Card';
import styles from './ExerciseCard.module.css';

const BADGES: Record<Exclude<ExerciseStatus, 'NORMAL'>, { label: string; variant: 'neutral' | 'inverse' | 'accent' }> = {
  OVERLOAD: { label: 'перегрузка', variant: 'accent' },
  PLATEAU: { label: 'плато', variant: 'neutral' },
  RECORD: { label: 'рекорд', variant: 'inverse' },
};

/** Карточка упражнения на главной. Детальная страница появится отдельной задачей — тогда карточка станет ссылкой. */
export function ExerciseCard({ exercise }: { exercise: ExerciseSummary }) {
  const badge = exercise.status === 'NORMAL' ? null : BADGES[exercise.status];
  return (
    <Card as="article" className={styles.card}>
      <div className={styles.head}>
        <h3 className={styles.name}>{exercise.exerciseName}</h3>
        {badge && <Badge variant={badge.variant}>{badge.label}</Badge>}
      </div>
      <span className={styles.set}>{formatSet(exercise.lastWeightKg, exercise.lastReps) ?? '—'}</span>
      <span className={styles.note}>{exercise.note ?? `последняя тренировка ${formatShortDate(exercise.lastPerformedAt)}`}</span>
    </Card>
  );
}
