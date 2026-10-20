import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { workoutsApi } from '../api/workouts';
import { ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { StatTile } from '../components/ui/StatTile';
import { formatTonnage } from '../lib/format';
import { describeSet, formatDuration } from '../lib/workout';
import styles from './WorkoutPages.module.css';

/** Экран "Тренировка завершена". Рекорды посчитает аналитика через пару секунд — они появятся на главной. */
export function WorkoutSummaryPage() {
  const { workoutId = '' } = useParams();
  const workout = useQuery({ queryKey: ['workout', workoutId], queryFn: () => workoutsApi.get(workoutId) });

  if (!workout.data) {
    return <p className={styles.muted}>{workout.isError ? workout.error.message : 'Загружаем…'}</p>;
  }
  const w = workout.data;
  const done = w.exercises.filter((e) => e.sets.length > 0);

  return (
    <div className={styles.page}>
      <div>
        <h1 className={styles.title}>Тренировка завершена</h1>
        <p className={styles.muted}>
          {w.name} · {formatDuration(w.durationSec ?? 0)}
        </p>
      </div>

      <div className={styles.tiles}>
        <StatTile variant="inverse" label="Поднято" value={formatTonnage(w.tonnageKg)} />
        <StatTile label="Подходов" value={w.workingSets} />
        <StatTile label="Упражнений" value={done.length} />
      </div>

      <Card>
        {done.map((exercise) => (
          <div key={exercise.id} className={styles.summaryExercise}>
            <span>{exercise.exerciseName}</span>
            <span className={styles.muted}>
              {exercise.sets
                .filter((s) => !s.warmup)
                .map((s) => describeSet(exercise.trackingType, s))
                .join(', ')}
            </span>
          </div>
        ))}
      </Card>

      <p className="footnote">Рекорды и прогресс обновятся на главной через несколько секунд.</p>
      <ButtonLink to="/">На главную</ButtonLink>
    </div>
  );
}
