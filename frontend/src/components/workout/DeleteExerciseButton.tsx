import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { workoutsApi, type CatalogExercise } from '../../api/workouts';
import { Button } from '../ui/Button';
import styles from './Workout.module.css';

/**
 * "Удалить упражнение" в карточке своего упражнения. Подтверждение — прямо на месте, без второго окна поверх.
 * Сервер сам решает, удалить совсем или спрятать (если по упражнению есть тренировки или комплексы),
 * поэтому в тексте честно говорим, что история не пропадёт.
 */
export function DeleteExerciseButton({ exercise, onDeleted }: { exercise: CatalogExercise; onDeleted: () => void }) {
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const remove = useMutation({
    mutationFn: () => workoutsApi.deleteExercise(exercise.id),
    onSuccess: () => {
      // ['exercises'] — префикс: обновятся все списки каталога, с любым поиском
      queryClient.invalidateQueries({ queryKey: ['exercises'] });
      onDeleted();
    },
  });

  if (!confirming) {
    return (
      <button type="button" className={styles.linkDanger} onClick={() => setConfirming(true)}>
        Удалить упражнение
      </button>
    );
  }

  return (
    <div className={styles.confirmBox} role="group" aria-label="Подтверждение удаления">
      <p className={styles.confirmText}>
        Удалить «{exercise.name}» из ваших упражнений? Если вы уже делали его, тренировки и графики сохранятся —
        упражнение просто пропадёт из каталога.
      </p>
      {remove.isError && (
        <p className={styles.error} role="alert">
          {remove.error.message}
        </p>
      )}
      <Button fullWidth className={styles.dangerButton} disabled={remove.isPending} onClick={() => remove.mutate()}>
        {remove.isPending ? 'Удаляем…' : 'Удалить'}
      </Button>
      <Button variant="secondary" fullWidth onClick={() => setConfirming(false)}>
        Отмена
      </Button>
    </div>
  );
}
