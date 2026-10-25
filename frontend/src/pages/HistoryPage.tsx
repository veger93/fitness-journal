import { useInfiniteQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { workoutsApi, type WorkoutHistoryItem } from '../api/workouts';
import { Button } from '../components/ui/Button';
import { formatTonnage } from '../lib/format';
import { formatDistance, formatDuration } from '../lib/workout';
import { exercisesLabel } from './StartWorkoutPage';
import styles from './ListPages.module.css';

const dayTitle = new Intl.DateTimeFormat('ru-RU', { weekday: 'short', day: 'numeric', month: 'long' });

/**
 * История тренировок, новые сверху. Подгружается страницами по 20 (useInfiniteQuery):
 * за год набирается 150+ тренировок — грузить всё сразу незачем.
 */
export function HistoryPage() {
  const history = useInfiniteQuery({
    queryKey: ['history'],
    queryFn: ({ pageParam }) => workoutsApi.history(pageParam),
    initialPageParam: 0,
    // следующая страница есть, пока не дошли до последней
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const items = history.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>История</h1>
      {history.isPending && <p className={styles.muted}>Загружаем…</p>}
      {history.isError && <p className={styles.muted}>{history.error.message}</p>}
      {history.data && items.length === 0 && <p className={styles.muted}>Здесь появятся завершённые тренировки.</p>}
      {items.length > 0 && (
        <ul className={styles.list}>
          {items.map((item) => (
            <li key={item.id}>
              <HistoryRow item={item} />
            </li>
          ))}
        </ul>
      )}
      {history.hasNextPage && (
        <Button variant="secondary" onClick={() => history.fetchNextPage()} disabled={history.isFetchingNextPage}>
          {history.isFetchingNextPage ? 'Загружаем…' : 'Показать ещё'}
        </Button>
      )}
    </div>
  );
}

function HistoryRow({ item }: { item: WorkoutHistoryItem }) {
  const durationSec = (new Date(item.completedAt).getTime() - new Date(item.startedAt).getTime()) / 1000;
  const facts = [
    exercisesLabel(item.exerciseCount),
    formatDuration(durationSec),
    item.tonnageKg > 0 && formatTonnage(item.tonnageKg),
    item.distanceM > 0 && formatDistance(item.distanceM),
  ].filter(Boolean);

  return (
    <Link to={`/history/${item.id}`} className={styles.historyRow}>
      <span className={styles.historyMain}>
        <span className={styles.historyTitle}>{item.name}</span>
        <span className={styles.muted}>{facts.join(' · ')}</span>
      </span>
      <span className={styles.historyDate}>{dayTitle.format(new Date(item.startedAt))}</span>
    </Link>
  );
}
