import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { workoutsApi, type WorkoutExercise } from '../../api/workouts';
import { EQUIPMENT_NAMES } from '../../lib/catalog';
import { BODY_PART_NAMES } from '../../lib/plan';
import { ExerciseImage } from '../ExerciseImage';
import { ExerciseInfo } from '../ExerciseInfo';
import { Dialog } from '../ui/Dialog';
import styles from './Workout.module.css';

/**
 * Карточка текущего упражнения на экране тренировки (вариант C): картинка, название, "грудь · штанга"
 * и "Как выполнять ›". Нажимается вся карточка — большая цель, попасть легко и не глядя.
 * Описание из каталога — тот же кэш, что у окна подхода и страницы упражнения (второго запроса не будет).
 */
export function ExerciseCard({ exercise }: { exercise: WorkoutExercise }) {
  const [open, setOpen] = useState(false);
  const catalog = useQuery({
    queryKey: ['exercise', exercise.exerciseId],
    queryFn: () => workoutsApi.exercise(exercise.exerciseId),
    staleTime: 60 * 60 * 1000,
  });
  const data = catalog.data;
  const meta = data
    ? `${BODY_PART_NAMES[data.bodyPart]} · ${EQUIPMENT_NAMES[data.equipment].toLowerCase()}`
    : EQUIPMENT_NAMES[exercise.equipment];

  return (
    <>
      <button
        type="button"
        className={styles.exerciseCard}
        onClick={() => setOpen(true)}
        disabled={!data}
        aria-label={`${exercise.exerciseName}. Как выполнять`}
      >
        <ExerciseImage imageKey={data?.imageKey ?? null} size="medium" />
        <span className={styles.exerciseCardText}>
          {/* заголовок страницы — название упражнения, как и было */}
          <h1 className={styles.exerciseCardTitle}>{exercise.exerciseName}</h1>
          <span className={styles.exerciseCardMeta}>{meta}</span>
          <span className={styles.exerciseCardLink}>Как выполнять ›</span>
        </span>
      </button>
      {data && (
        <Dialog open={open} onClose={() => setOpen(false)} title={exercise.exerciseName}>
          <ExerciseInfo exercise={data} />
        </Dialog>
      )}
    </>
  );
}
