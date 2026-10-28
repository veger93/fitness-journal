import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { workoutsApi, type SetInput, type WorkoutExercise, type WorkoutSet } from '../../api/workouts';
import { formatKg, formatShortDate } from '../../lib/format';
import { distanceFromSpeed, formatPace, formatSpeed, speedKmh, suggestNextSet } from '../../lib/workout';
import { ExerciseImage } from '../ExerciseImage';
import { ExerciseInfo } from '../ExerciseInfo';
import { SegmentedControl } from '../ui/SegmentedControl';
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
 * Модалка "Добавление подхода": поля под вид упражнения (вес/время/бег), разминка, Готово/Отмена.
 * Слева в шапке — миниатюра с "i": открывает карточку упражнения прямо в этом окне.
 * Введённые вес и повторы живут в state этого компонента, поэтому после "←" они на месте.
 */
export function SetDialog({ exercise, editing, saving, error, onSave, onDelete, onClose }: SetDialogProps) {
  const [values, setValues] = useState<SetInput>(
    () =>
      editing ??
      suggestNextSet(exercise.sets, exercise.previousSets, exercise.trackingType, exercise.equipment, exercise.deload),
  );
  const change = (patch: Partial<SetInput>) => setValues((v) => ({ ...v, ...patch }));
  const [showInfo, setShowInfo] = useState(false);
  // описание упражнения из каталога (картинка, мышцы) — тот же кэш, что у страницы упражнения
  const catalogExercise = useQuery({
    queryKey: ['exercise', exercise.exerciseId],
    queryFn: () => workoutsApi.exercise(exercise.exerciseId),
    staleTime: 60 * 60 * 1000,
  });

  const number = editing ? editing.number : exercise.sets.length + 1;
  const isRun = exercise.trackingType === 'DISTANCE';
  // ни сегодня, ни раньше — подсказка, как выбрать стартовый вес
  const firstTime = !editing && exercise.sets.length === 0 && exercise.previousSets.length === 0;
  const planned = exercise.previousSets.length;
  const deload = exercise.trackingType === 'WEIGHT_REPS' ? exercise.deload : null;
  const subtitle =
    deload && !editing
      ? `Подход ${number} из ${Math.max(deload.sets, number)} · разгрузка`
      : isRun
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

  if (showInfo && catalogExercise.data) {
    return (
      <Dialog open onClose={onClose} onBack={() => setShowInfo(false)} title={exercise.exerciseName}>
        <ExerciseInfo exercise={catalogExercise.data} />
        <Button fullWidth onClick={() => setShowInfo(false)}>
          К подходу
        </Button>
      </Dialog>
    );
  }

  const leading = catalogExercise.data && (
    <button
      type="button"
      className={styles.infoThumb}
      aria-label={`Как выполнять: ${exercise.exerciseName}`}
      onClick={() => setShowInfo(true)}
    >
      <ExerciseImage imageKey={catalogExercise.data.imageKey} />
      <span className={styles.infoBadge} aria-hidden="true">
        i
      </span>
    </button>
  );

  return (
    <Dialog open onClose={onClose} title={exercise.exerciseName} subtitle={subtitle} leading={leading}>
      <form className={styles.form} onSubmit={submit}>
        {exercise.trackingType === 'WEIGHT_REPS' && (
          <>
            <Stepper
              label={exercise.equipment === 'BODYWEIGHT' ? 'Доп. вес, кг (0 — без веса)' : 'Вес, кг'}
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
        {isRun && <RunFields values={values} change={change} />}

        {deload && !editing && (
          <p className="footnote">
            Разгрузка до {formatShortDate(deload.endsAt)}:{' '}
            {deload.workingWeightKg != null
              ? `${formatKg(deload.workingWeightKg)} × ${deload.reps}`
              : `${deload.reps} повт.`}
            , {deload.sets} подхода. Лёгкий вес — это и есть план: так быстрее уходит усталость.
          </p>
        )}

        {exercise.trackingType === 'WEIGHT_REPS' && firstTime && !deload && (
          <p className="footnote">
            Впервые в этом упражнении: возьмите вес, с которым 10–12 повторов даются с запасом 2–3 повтора. В следующий
            раз приложение подставит ваш результат.
          </p>
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

type RunInput = 'speed' | 'distance';

const RUN_INPUT_KEY = 'vegas.runInput';
const RUN_INPUTS: { value: RunInput; label: string }[] = [
  { value: 'speed', label: 'Скорость и время' },
  { value: 'distance', label: 'Дистанция и время' },
];

/**
 * "Как далась пробежка?" — субъективная тяжесть (RPE), которую мы храним в подходе.
 * Простой ориентир — "тест разговором": легко, если можно спокойно говорить; тяжело — если почти нельзя.
 * Нужна, чтобы потом подсказать баланс: у бегунов большая часть пробежек — лёгкие (Seiler, 2010).
 */
const EFFORT = [
  { label: 'Легко', rpe: 4 },
  { label: 'Средне', rpe: 6 },
  { label: 'Тяжело', rpe: 8 },
];

/**
 * Два способа записать пробежку — как удобнее:
 * беговая дорожка показывает скорость и время, часы на улице — дистанцию и время.
 * Храним всегда дистанцию и время: из любых двух величин третья считается
 * (дистанция = скорость × время), так что для аналитики способы равноценны.
 */
function RunFields({ values, change }: { values: SetInput; change: (patch: Partial<SetInput>) => void }) {
  const [mode, setMode] = useState<RunInput>(() => {
    try {
      return localStorage.getItem(RUN_INPUT_KEY) === 'distance' ? 'distance' : 'speed';
    } catch {
      return 'speed';
    }
  });
  const duration = values.durationSec ?? 0;
  const distance = values.distanceM ?? 0;
  // скорость в поле — с точностью 0,1 км/ч, как на дорожке
  const speed = duration && distance ? Math.round(speedKmh(duration, distance) * 10) / 10 : 8;

  const switchMode = (next: RunInput) => {
    setMode(next);
    try {
      localStorage.setItem(RUN_INPUT_KEY, next);
    } catch {
      // не запомнили — не страшно
    }
  };

  return (
    <>
      <SegmentedControl legend="Как записать пробежку" options={RUN_INPUTS} value={mode} onChange={switchMode} />
      {mode === 'speed' ? (
        <>
          <Stepper
            label="Средняя скорость, км/ч"
            value={speed}
            step={0.1}
            max={40}
            inputMode="decimal"
            onChange={(kmh) => change({ distanceM: distanceFromSpeed(kmh, duration) })}
          />
          <TimeField
            legend="Время"
            seconds={duration}
            onChange={(durationSec) => change({ durationSec, distanceM: distanceFromSpeed(speed, durationSec) })}
          />
        </>
      ) : (
        <>
          <Stepper
            label="Дистанция, км"
            value={distance / 1000}
            step={0.1}
            max={1000}
            inputMode="decimal"
            onChange={(km) => change({ distanceM: Math.round(km * 1000) })}
          />
          <TimeField legend="Время" seconds={duration} onChange={(durationSec) => change({ durationSec })} />
        </>
      )}

      {duration > 0 && distance > 0 && (
        <div className={styles.effortBlock}>
          <p className={styles.pace}>
            {mode === 'speed' ? `Дистанция ${formatKm(distance)}` : formatSpeed(duration, distance)} · темп{' '}
            {formatPace(duration, distance)}
          </p>
          <p className="footnote">Темп — сколько минут уходит на 1 км: чем он меньше, тем быстрее бег.</p>
        </div>
      )}

      <div className={styles.effortBlock}>
        <span className={styles.effortQuestion}>Как далась пробежка?</span>
        <div className={styles.effort} role="group" aria-label="Как далась пробежка">
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
        <p className="footnote">
          Легко — можно спокойно разговаривать, тяжело — говорить почти не получается. Необязательно: поможет
          подсказать, хватает ли лёгких пробежек.
        </p>
      </div>
    </>
  );
}

function formatKm(meters: number): string {
  return `${new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(meters / 1000)} км`;
}
