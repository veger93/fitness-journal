import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useState } from 'react';
import { workoutsApi } from '../../api/workouts';
import { Dialog } from '../ui/Dialog';
import { TextField } from '../ui/TextField';
import styles from './Workout.module.css';

interface AddExerciseDialogProps {
  /** уже есть в тренировке — помечаем, чтобы не добавить дважды по ошибке */
  existingIds: string[];
  adding: boolean;
  onAdd: (exerciseId: string) => void;
  onClose: () => void;
}

export function AddExerciseDialog({ existingIds, adding, onAdd, onClose }: AddExerciseDialogProps) {
  const [search, setSearch] = useState('');
  // useDeferredValue: пока печатаешь, список обновляется "с отставанием" и не дёргает сервер на каждую букву так часто
  const deferred = useDeferredValue(search.trim());
  const exercises = useQuery({ queryKey: ['exercises', deferred], queryFn: () => workoutsApi.exercises(deferred) });

  return (
    <Dialog open onClose={onClose} title="Добавить упражнение">
      <TextField
        label="Поиск"
        type="search"
        placeholder="например, жим"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        autoFocus
      />
      <ul className={styles.catalog}>
        {exercises.data?.map((exercise) => {
          const already = existingIds.includes(exercise.id);
          return (
            <li key={exercise.id}>
              <button
                type="button"
                className={styles.catalogItem}
                disabled={adding || already}
                onClick={() => onAdd(exercise.id)}
              >
                <span>{exercise.name}</span>
                {already && <span className={styles.muted}>уже в тренировке</span>}
              </button>
            </li>
          );
        })}
        {exercises.data?.length === 0 && <li className={styles.muted}>Ничего не нашлось</li>}
        {exercises.isPending && <li className={styles.muted}>Ищем…</li>}
      </ul>
    </Dialog>
  );
}
