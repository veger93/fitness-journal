import { useState, type FormEvent } from 'react';
import type { SetInput, WorkoutExercise, WorkoutSet } from '../../api/workouts';
import { formatPace, suggestNextSet } from '../../lib/workout';
import { Chip } from '../ui/Chip';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { Stepper } from '../ui/Stepper';
import { TimeField } from '../ui/TimeField';
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

/**
 * "Как далось" для бега — субъективная тяжесть (RPE), которую мы уже храним в подходе.
 * Пригодится для подсказки про баланс лёгких и тяжёлых пробежек (Seiler, 2010: ~80% лёгких).
 */
const EFFORT = [
  { label: 'Легко', rpe: 4 },
  { label: 'Средне', rpe: 6 },
  { label: 'Тяжело', rpe: 8 },
];

/** Модалка "Добавление подхода": поля под вид упражнения (вес/время/бег), разминка, Готово/Отмена. */
export function SetDialog({ exercise, editing, saving, error, onSave, onDelete, onClose }: SetDialogProps) {
  const [values, setValues] = useState<SetInput>(
    () => editing ?? suggestNextSet(exercise.sets, exercise.previousSets, exercise.trackingType),
  );
  const change = (patch: Partial<SetInput>) => setValues((v) => ({ ...v, ...patch }));

  const number = editing ? editing.number : exercise.sets.length + 1;
  const isRun = exercise.trackingType === 'DISTANCE';
  const planned = exercise.previousSets.length;
  const subtitle = isRun
    ? number === 1 && exercise.sets.length <= 1
      ? 'Пробежка'
      : `Отрезок ${number}`
    : planned > 0 && !editing
      ? `Подход ${number} из ~${Math.max(planned, number)}`
      : `Подход ${number}`;

  const invalid =
    (exercise.trackingType === 'WEIGHT_REPS' && (Number.isNaN(values.weightKg) || !values.reps)) ||
    (exercise.trackingType === 'TIME' && !values.durationSec) ||
    (exercise.trackingType === 'DISTANCE' && (!values.distanceM || !values.durationSec));

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
          <TimeField
            legend="Время"
            seconds={values.durationSec ?? 0}
            onChange={(durationSec) => change({ durationSec })}
          />
        )}
        {isRun && (
          <>
            <Stepper
              label="Дистанция, км"
              value={(values.distanceM ?? 0) / 1000}
              step={0.1}
              max={1000}
              inputMode="decimal"
              onChange={(km) => change({ distanceM: Math.round(km * 1000) })}
            />
            <TimeField
              legend="Время"
              seconds={values.durationSec ?? 0}
              onChange={(durationSec) => change({ durationSec })}
            />
            {values.distanceM && values.durationSec ? (
              <p className={styles.pace}>Темп {formatPace(values.durationSec, values.distanceM)}</p>
            ) : null}
            <div className={styles.effort} role="group" aria-label="Как далось">
              {EFFORT.map((option) => (
                <Chip
                  key={option.rpe}
                  selected={values.rpe === option.rpe}
                  onClick={() => change({ rpe: values.rpe === option.rpe ? null : option.rpe })}
                >
                  {option.label}
                </Chip>
              ))}
            </div>
          </>
        )}

        <label className={styles.checkbox}>
          <input type="checkbox" checked={values.warmup} onChange={(e) => change({ warmup: e.target.checked })} />
          {isRun ? 'Разминка или заминка*' : 'Разминочный подход*'}
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
