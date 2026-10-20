import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../api/analytics';
import { ExerciseRow } from '../components/ExerciseRow';
import styles from './ListPages.module.css';

/** "Все упражнения": те же строки, что в полосе на главной, но списком. Данные — из того же /dashboard (кэш общий). */
export function ExercisesPage() {
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: analyticsApi.dashboard });
  const exercises = dashboard.data?.exercises ?? [];

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Упражнения</h1>
      {dashboard.isPending && <p className={styles.muted}>Загружаем…</p>}
      {dashboard.isError && <p className={styles.muted}>{dashboard.error.message}</p>}
      {dashboard.data && exercises.length === 0 && (
        <p className={styles.muted}>Здесь появятся упражнения после первой тренировки.</p>
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
