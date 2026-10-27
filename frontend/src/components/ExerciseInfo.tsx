import type { CatalogExercise } from '../api/workouts';
import { musclesText } from '../lib/exercise';
import { BODY_PART_NAMES } from '../lib/plan';
import { ExerciseImage } from './ExerciseImage';
import styles from './ExerciseInfo.module.css';

/**
 * Карточка упражнения для новичка: два положения движения и какие мышцы работают.
 * Это только содержимое — в окно его кладёт тот, кто показывает (окно выбора упражнения, страница упражнения).
 */
export function ExerciseInfo({ exercise }: { exercise: CatalogExercise }) {
  const primary = musclesText(exercise.muscles, 'PRIMARY');
  const secondary = musclesText(exercise.muscles, 'SECONDARY');

  return (
    <div className={styles.info}>
      {exercise.imageKey ? (
        <div className={styles.phases}>
          <ExerciseImage imageKey={exercise.imageKey} phase={1} size="large" alt={`${exercise.name}: положение 1`} />
          {/* ⇄ — движение туда и обратно */}
          <span className={styles.arrow} aria-hidden="true">
            ⇄
          </span>
          <ExerciseImage imageKey={exercise.imageKey} phase={2} size="large" alt={`${exercise.name}: положение 2`} />
        </div>
      ) : (
        <p className={styles.muted}>Картинки для этого упражнения пока нет.</p>
      )}

      <dl className={styles.muscles}>
        <dt>Часть тела</dt>
        <dd>{BODY_PART_NAMES[exercise.bodyPart]}</dd>
        {primary && (
          <>
            <dt>Работают</dt>
            <dd>{primary}</dd>
          </>
        )}
        {secondary && (
          <>
            <dt>Помогают</dt>
            <dd>{secondary}</dd>
          </>
        )}
      </dl>

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
