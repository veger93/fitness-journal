import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../api/analytics';
import { ExerciseRow } from '../components/ExerciseRow';
import { EmptyState } from '../components/ui/EmptyState';
import { PageHeader } from '../components/ui/PageHeader';
import { plural } from '../lib/format';
import styles from './ListPages.module.css';

/** "Все упражнения": те же строки, что в полосе на главной, но списком. Данные — из того же /dashboard (кэш общий). */
export function ExercisesPage() {
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: analyticsApi.dashboard });
  const exercises = dashboard.data?.exercises ?? [];

  return (
    <div className={styles.page}>
      <PageHeader
        title="Упражнения"
        subtitle={
          exercises.length > 0
            ? `${plural(exercises.length, 'упражнение', 'упражнения', 'упражнений')} · последний подход и тренд`
            : undefined
        }
      />
      {dashboard.isPending && <p className={styles.muted}>Загружаем…</p>}
      {dashboard.isError && <p className={styles.muted}>{dashboard.error.message}</p>}
      {dashboard.data && exercises.length === 0 && (
        <EmptyState
          title="Упражнений пока нет"
          text="Здесь появятся упражнения после первой тренировки — с прогрессом и подсказками по каждому."
          action={{ to: '/workout', label: 'Начать тренировку' }}
        />
      )}
      {exercises.length > 0 && (
        <ul className={styles.list}>
          {exercises.map((exercise) => (
            <li key={exercise.exerciseId}>
              <ExerciseRow exercise={exercise} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
