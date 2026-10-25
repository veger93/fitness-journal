import { useInfiniteQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { workoutsApi, type Workout } from '../api/workouts';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { formatTonnage } from '../lib/format';
import { formatDistance, formatDuration, summarizeSets } from '../lib/workout';
import styles from './ListPages.module.css';

const dayFormat = new Intl.DateTimeFormat('ru-RU', {
  weekday: 'short',
  day: '2-digit',
  month: '2-digit',
  year: '2-digit',
});

/**
 * История как дневник: по карточке на тренировку — дата, комплекс и сразу все упражнения с весами.
 * Чтобы вспомнить "что я делал в четверг", не нужно открывать каждую тренировку.
 * Подгружается по 10 тренировок (useInfiniteQuery).
 */
export function HistoryPage() {
  const journal = useInfiniteQuery({
    queryKey: ['history'],
    queryFn: ({ pageParam }) => workoutsApi.journal(pageParam),
    initialPageParam: 0,
    // следующая страница есть, пока не дошли до последней
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const workouts = journal.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>История</h1>
      {journal.isPending && <p className={styles.muted}>Загружаем…</p>}
      {journal.isError && <p className={styles.muted}>{journal.error.message}</p>}
      {journal.data && workouts.length === 0 && <p className={styles.muted}>Здесь появятся завершённые тренировки.</p>}

      {workouts.map((workout) => (
        <JournalDay key={workout.id} workout={workout} />
      ))}

      {journal.hasNextPage && (
        <Button variant="secondary" onClick={() => journal.fetchNextPage()} disabled={journal.isFetchingNextPage}>
          {journal.isFetchingNextPage ? 'Загружаем…' : 'Показать ещё'}
        </Button>
      )}
    </div>
  );
}

function JournalDay({ workout }: { workout: Workout }) {
  const exercises = workout.exercises.filter((e) => e.sets.length > 0);
  const distanceM = exercises
    .filter((e) => e.trackingType === 'DISTANCE')
    .flatMap((e) => e.sets.filter((s) => !s.warmup))
    .reduce((sum, s) => sum + (s.distanceM ?? 0), 0);
  const facts = [
    formatDuration(workout.durationSec ?? 0),
    workout.tonnageKg > 0 && formatTonnage(workout.tonnageKg),
    distanceM > 0 && formatDistance(distanceM),
  ].filter(Boolean);

  return (
    <Card as="article">
      <header className={styles.dayHead}>
        <h2 className={styles.dayTitle}>
          {dayFormat.format(new Date(workout.startedAt ?? workout.completedAt ?? ''))}
        </h2>
        <span className={styles.muted}>
          {workout.name} · {facts.join(' · ')}
        </span>
      </header>
      <ul className={styles.dayExercises}>
        {exercises.map((exercise) => (
          <li key={exercise.id} className={styles.dayExercise}>
            <span className={styles.dayExerciseName}>{exercise.exerciseName}</span>
            <span>{summarizeSets(exercise.trackingType, exercise.sets)}</span>
          </li>
        ))}
      </ul>
      <Link to={`/history/${workout.id}`} className={styles.dayLink}>
        Подробнее →
      </Link>
    </Card>
  );
}
