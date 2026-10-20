import { useState, type FormEvent } from 'react';
import type { SetInput, WorkoutExercise, WorkoutSet } from '../../api/workouts';
import { suggestNextSet } from '../../lib/workout';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { Stepper } from '../ui/Stepper';
import styles from './Workout.module.css';

interface SetDialogProps {
  exercise: WorkoutExercise;
  /** null — новый подход, иначе — исправляем существующий */
  editing: WorkoutSet | null;
  saving: boolean;
  error: string | null;
  onSave: (set: SetInput) => void;
  onDelete: () => void;
  onClose: () => void;
}

/** Модалка "Добавление подхода": степперы под вид упражнения, разминка, Готово/Отмена. */
export function SetDialog({ exercise, editing, saving, error, onSave, onDelete, onClose }: SetDialogProps) {
  const [values, setValues] = useState<SetInput>(() => editing ?? suggestNextSet(exercise.sets, exercise.previousSets));
  const change = (patch: Partial<SetInput>) => setValues((v) => ({ ...v, ...patch }));

  const number = editing ? editing.number : exercise.sets.length + 1;
  const planned = exercise.previousSets.length;
  const subtitle = planned > 0 && !editing ? `Подход ${number} из ~${Math.max(planned, number)}` : `Подход ${number}`;

  const invalid =
    (exercise.trackingType === 'WEIGHT_REPS' && (Number.isNaN(values.weightKg) || !values.reps)) ||
    (exercise.trackingType === 'TIME' && !values.durationSec) ||
    (exercise.trackingType === 'DISTANCE' && !values.distanceM);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!invalid) {
      onSave(values);
    }
  };

  return (
    <Dialog open onClose={onClose} title={exercise.exerciseName} subtitle={subtitle}>
      <form className={styles.form} onSubmit={submit}>
        {exercise.trackingType === 'WEIGHT_REPS' && (
          <>
            <Stepper
              label="Вес, кг"
              value={values.weightKg ?? 0}
              step={2.5}
              max={9999}
              inputMode="decimal"
              onChange={(weightKg) => change({ weightKg })}
            />
            <Stepper
              label="Повторы"
              value={values.reps ?? 0}
              step={1}
              min={1}
              max={1000}
              onChange={(reps) => change({ reps })}
            />
          </>
        )}
        {exercise.trackingType === 'TIME' && (
          <Stepper
            label="Время, сек"
            value={values.durationSec ?? 0}
            step={5}
            min={1}
            max={86_400}
            onChange={(durationSec) => change({ durationSec })}
          />
        )}
        {exercise.trackingType === 'DISTANCE' && (
          <Stepper
            label="Дистанция, м"
            value={values.distanceM ?? 0}
            step={50}
            min={1}
            max={1_000_000}
            onChange={(distanceM) => change({ distanceM })}
          />
        )}

        <label className={styles.checkbox}>
          <input type="checkbox" checked={values.warmup} onChange={(e) => change({ warmup: e.target.checked })} />
          Разминочный подход*
        </label>

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <Button type="submit" fullWidth disabled={saving || invalid}>
          {saving ? 'Сохраняем…' : 'Готово'}
        </Button>
        <Button variant="secondary" fullWidth onClick={onClose}>
          Отмена
        </Button>
        {editing && (
          <button type="button" className={styles.linkDanger} onClick={onDelete} disabled={saving}>
            Удалить подход
          </button>
        )}
        <p className="footnote">* Разминка не идёт в рекорды и статистику.</p>
      </form>
    </Dialog>
  );
}
