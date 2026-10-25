import { Link } from 'react-router-dom';
import type { ExerciseStatus, ExerciseSummary } from '../api/analytics';
import { formatExerciseResult, formatShortDate } from '../lib/format';
import { Badge } from './ui/Badge';
import { Sparkline } from './ui/Sparkline';
import styles from './ExerciseRow.module.css';

const BADGES: Record<
  Exclude<ExerciseStatus, 'NORMAL'>,
  { label: string; variant: 'neutral' | 'inverse' | 'accent' }
> = {
  OVERLOAD: { label: 'перегрузка', variant: 'accent' },
  PLATEAU: { label: 'плато', variant: 'neutral' },
  RECORD: { label: 'рекорд', variant: 'inverse' },
};

/** Строка упражнения: название и пояснение · последний подход · мини-график · вердикт. Главная и "Все упражнения". */
export function ExerciseRow({ exercise }: { exercise: ExerciseSummary }) {
  const badge = exercise.status === 'NORMAL' ? null : BADGES[exercise.status];
  const content = (
    <>
      <div className={styles.name}>
        <span className={styles.title}>{exercise.exerciseName}</span>
        <span className={styles.note}>
          {exercise.note ?? `последняя тренировка ${formatShortDate(exercise.lastPerformedAt)}`}
        </span>
      </div>
      <span className={styles.set}>{formatExerciseResult(exercise) ?? '—'}</span>
      <span className={styles.spark}>
        <Sparkline values={exercise.trend} accent={exercise.status === 'OVERLOAD'} />
      </span>
      <span className={styles.badge}>{badge && <Badge variant={badge.variant}>{badge.label}</Badge>}</span>
    </>
  );
  // бег ведёт на страницу "Бег"; силовые станут ссылками вместе с детальной страницей упражнения
  return exercise.trackingType === 'DISTANCE' ? (
    <Link to="/running" className={`${styles.row} ${styles.link}`}>
      {content}
    </Link>
  ) : (
    <div className={styles.row}>{content}</div>
  );
}
