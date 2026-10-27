import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useState } from 'react';
import { workoutsApi, type CatalogExercise } from '../../api/workouts';
import { musclesText } from '../../lib/exercise';
import { ExerciseImage } from '../ExerciseImage';
import { ExerciseInfo } from '../ExerciseInfo';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { TextField } from '../ui/TextField';
import { NewExerciseForm } from './NewExerciseForm';
import styles from './Workout.module.css';

interface AddExerciseDialogProps {
  /** уже есть в тренировке — помечаем, чтобы не добавить дважды по ошибке */
  existingIds: string[];
  adding: boolean;
  onAdd: (exerciseId: string) => void;
  onClose: () => void;
}

/**
 * Поиск по каталогу. У каждого упражнения миниатюра и главные мышцы, по "i" — карточка с картинками,
 * чтобы новичок понял, что это за упражнение, до того как добавит.
 * Нет нужного — тут же создаём своё упражнение и сразу добавляем в тренировку.
 */
export function AddExerciseDialog({ existingIds, adding, onAdd, onClose }: AddExerciseDialogProps) {
  const [search, setSearch] = useState('');
  const [creating, setCreating] = useState(false);
  // открытая карточка упражнения; поиск при этом не сбрасываем — "к списку" вернёт туда же
  const [info, setInfo] = useState<CatalogExercise | null>(null);
  // useDeferredValue: пока печатаешь, список обновляется "с отставанием" и не дёргает сервер на каждую букву так часто
  const deferred = useDeferredValue(search.trim());
  const exercises = useQuery({ queryKey: ['exercises', deferred], queryFn: () => workoutsApi.exercises(deferred) });

  if (creating) {
    return (
      <Dialog open onClose={onClose} title="Своё упражнение">
        <NewExerciseForm
          initialName={search.trim()}
          onCreated={(exercise) => onAdd(exercise.id)}
          onCancel={() => setCreating(false)}
        />
      </Dialog>
    );
  }

  if (info) {
    const already = existingIds.includes(info.id);
    return (
      <Dialog open onClose={onClose} title={info.name}>
        <ExerciseInfo exercise={info} />
        <div className={styles.infoActions}>
          <Button fullWidth disabled={adding || already} onClick={() => onAdd(info.id)}>
            {already ? 'Уже в тренировке' : 'Добавить в тренировку'}
          </Button>
          <Button variant="secondary" fullWidth onClick={() => setInfo(null)}>
            ← К списку
          </Button>
        </div>
      </Dialog>
    );
  }

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
          const primary = musclesText(exercise.muscles, 'PRIMARY');
          return (
            // строка — это две кнопки рядом: "добавить" и "что это". Кнопку в кнопку вкладывать нельзя
            <li key={exercise.id} className={styles.catalogRow}>
              <button
                type="button"
                className={styles.catalogItem}
                disabled={adding || already}
                onClick={() => onAdd(exercise.id)}
              >
                <ExerciseImage imageKey={exercise.imageKey} />
                <span className={styles.catalogText}>
                  <span>
                    {exercise.name}
                    {exercise.custom && <span className={styles.muted}> · своё</span>}
                  </span>
                  <span className={styles.muted}>{already ? 'уже в тренировке' : primary}</span>
                </span>
              </button>
              <button
                type="button"
                className={styles.infoButton}
                aria-label={`Как выполнять: ${exercise.name}`}
                onClick={() => setInfo(exercise)}
              >
                i
              </button>
            </li>
          );
        })}
        {exercises.data?.length === 0 && <li className={styles.muted}>Ничего не нашлось</li>}
        {exercises.isPending && <li className={styles.muted}>Ищем…</li>}
      </ul>
      <button type="button" className={styles.createLink} onClick={() => setCreating(true)}>
        Нет нужного? Создать своё упражнение
      </button>
    </Dialog>
  );
}
