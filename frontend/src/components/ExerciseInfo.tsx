import { useQuery } from '@tanstack/react-query';
import { workoutsApi, type CatalogExercise } from '../api/workouts';
import { EQUIPMENT_NAMES } from '../lib/catalog';
import { MUSCLE_NAMES } from '../lib/exercise';
import { BODY_PART_NAMES } from '../lib/plan';
import { ExerciseImage } from './ExerciseImage';
import styles from './ExerciseInfo.module.css';

/**
 * Карточка упражнения для новичка: два положения движения, какие мышцы работают,
 * исходное положение, шаги и частые ошибки.
 * Это только содержимое — в окно его кладёт тот, кто показывает (выбор упражнения, окно подхода, страница упражнения).
 * Техника грузится отдельным запросом: в каталоге её нет, чтобы список упражнений оставался лёгким.
 */
export function ExerciseInfo({ exercise }: { exercise: CatalogExercise }) {
  const technique = useQuery({
    queryKey: ['technique', exercise.id],
    queryFn: () => workoutsApi.technique(exercise.id),
    // тексты техники меняются только миграциями — в рамках сессии их можно не перезапрашивать
    staleTime: Infinity,
  });

  return (
    <div className={styles.info}>
      <p className={styles.kind}>
        {BODY_PART_NAMES[exercise.bodyPart]} · {EQUIPMENT_NAMES[exercise.equipment].toLowerCase()}
      </p>

      {exercise.imageKey && (
        <div className={styles.phases}>
          <ExerciseImage imageKey={exercise.imageKey} phase={1} size="large" alt={`${exercise.name}: положение 1`} />
          {/* ⇄ — движение туда и обратно */}
          <span className={styles.arrow} aria-hidden="true">
            ⇄
          </span>
          <ExerciseImage imageKey={exercise.imageKey} phase={2} size="large" alt={`${exercise.name}: положение 2`} />
        </div>
      )}

      {exercise.muscles.length > 0 && (
        // тёмный чип — основная работа, серый — помогают
        <ul className={styles.muscles} aria-label="Мышцы">
          {exercise.muscles.map((m) => (
            <li key={m.muscle} className={m.role === 'PRIMARY' ? styles.primary : styles.secondary}>
              {MUSCLE_NAMES[m.muscle]}
            </li>
          ))}
        </ul>
      )}

      {technique.isPending && <p className={styles.muted}>Загружаем технику…</p>}
      {technique.isError && <p className={styles.muted}>{technique.error.message}</p>}
      {technique.data && (
        <>
          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>Исходное положение</h3>
            <p className={styles.text}>{technique.data.setup}</p>
          </section>
          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>Как выполнять</h3>
            <ol className={styles.steps}>
              {technique.data.steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
          </section>
          <section className={`${styles.section} ${styles.mistakes}`}>
            <h3 className={styles.sectionTitle}>Частые ошибки</h3>
            <ul className={styles.steps}>
              {technique.data.mistakes.map((mistake) => (
                <li key={mistake}>{mistake}</li>
              ))}
            </ul>
          </section>
        </>
      )}
      {technique.data === null && !exercise.imageKey && (
        <p className={styles.muted}>Описания техники для этого упражнения пока нет.</p>
      )}

      {exercise.imageKey && (
        // лицензия CC BY-SA требует указать автора и лицензию там, где показываем картинки
        <p className={styles.credit}>
          Иллюстрации:{' '}
          <a href="https://github.com/everkinetic/data" target="_blank" rel="noreferrer">
            Everkinetic
          </a>
          ,{' '}
          <a href="https://creativecommons.org/licenses/by-sa/4.0/deed.ru" target="_blank" rel="noreferrer">
            CC BY-SA
          </a>
        </p>
      )}
    </div>
  );
}
