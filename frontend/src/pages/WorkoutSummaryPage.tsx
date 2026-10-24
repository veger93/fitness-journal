import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { workoutsApi } from '../api/workouts';
import { Button, ButtonLink } from '../components/ui/Button';
import { SaveTemplateDialog } from '../components/workout/SaveTemplateDialog';
import { Card } from '../components/ui/Card';
import { StatTile } from '../components/ui/StatTile';
import { formatTonnage } from '../lib/format';
import { describeSet, formatDistance, formatDuration } from '../lib/workout';
import styles from './WorkoutPages.module.css';

interface Tile {
  label: string;
  value: string | number;
}

/** Экран "Тренировка завершена". Рекорды посчитает аналитика через пару секунд — они появятся на главной. */
const dayTitle = new Intl.DateTimeFormat('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' });

/**
 * Одна завершённая тренировка. Два режима:
 * "finished" — сразу после "Завершить" ("Тренировка завершена");
 * "view" — открыта из истории (заголовок — дата, можно вспомнить, что и с каким весом делал).
 */
export function WorkoutSummaryPage({ mode = 'finished' }: { mode?: 'finished' | 'view' }) {
  const { workoutId = '' } = useParams();
  const [saving, setSaving] = useState(false);
  const workout = useQuery({ queryKey: ['workout', workoutId], queryFn: () => workoutsApi.get(workoutId) });

  if (!workout.data) {
    return <p className={styles.muted}>{workout.isError ? workout.error.message : 'Загружаем…'}</p>;
  }
  const w = workout.data;
  const done = w.exercises.filter((e) => e.sets.length > 0);
  const workingSets = (type: 'run' | 'other') =>
    done
      .filter((e) => (e.trackingType === 'DISTANCE') === (type === 'run'))
      .flatMap((e) => e.sets.filter((s) => !s.warmup));
  const distanceM = workingSets('run').reduce((sum, s) => sum + (s.distanceM ?? 0), 0);
  const otherSets = workingSets('other').length;

  // показываем только то, что было в тренировке: у пробежки нет "Поднято 0 кг"
  const tiles: (Tile | false)[] = [
    w.tonnageKg > 0 && { label: 'Поднято', value: formatTonnage(w.tonnageKg) },
    distanceM > 0 && { label: 'Дистанция', value: formatDistance(distanceM) },
    otherSets > 0 && { label: 'Подходов', value: otherSets },
    { label: 'Упражнений', value: done.length },
  ];
  const shown = tiles.filter((tile): tile is Tile => tile !== false);

  return (
    <div className={styles.page}>
      <div>
        <h1 className={styles.title}>
          {mode === 'finished'
            ? 'Тренировка завершена'
            : capitalize(dayTitle.format(new Date(w.startedAt ?? w.completedAt ?? '')))}
        </h1>
        <p className={styles.muted}>
          {w.name} · {formatDuration(w.durationSec ?? 0)}
        </p>
      </div>

      <div className={styles.tiles}>
        {shown.map((tile, index) => (
          // первая плитка — главная цифра тренировки, она акцентная
          <StatTile
            key={tile.label}
            variant={index === 0 ? 'inverse' : 'default'}
            label={tile.label}
            value={tile.value}
          />
        ))}
      </div>

      {done.map((exercise) => (
        <Card key={exercise.id} as="section">
          <h2 className={styles.exerciseTitle}>{exercise.exerciseName}</h2>
          <ol className={styles.setList}>
            {exercise.sets.map((set) => (
              <li key={set.id} className={styles.summaryExercise}>
                <span className={styles.muted}>
                  {exercise.trackingType === 'DISTANCE' && exercise.sets.length === 1 ? 'Пробежка' : `${set.number}`}
                  {set.warmup && ' · разминка'}
                </span>
                <span className={styles.setValueSmall}>{describeSet(exercise.trackingType, set)}</span>
              </li>
            ))}
          </ol>
        </Card>
      ))}

      {mode === 'finished' && (
        <p className="footnote">Рекорды и прогресс обновятся на главной через несколько секунд.</p>
      )}
      {!w.templateId && (
        <Button variant="secondary" onClick={() => setSaving(true)}>
          Сохранить как комплекс
        </Button>
      )}
      {mode === 'finished' ? (
        <ButtonLink to="/">На главную</ButtonLink>
      ) : (
        <ButtonLink to="/history" variant="secondary">
          ← К истории
        </ButtonLink>
      )}
      {saving && <SaveTemplateDialog exerciseIds={done.map((e) => e.exerciseId)} onClose={() => setSaving(false)} />}
    </div>
  );
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
