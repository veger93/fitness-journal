import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { workoutsApi } from '../api/workouts';
import { CURRENT_WORKOUT_KEY } from '../hooks/useWorkout';
import { Button, ButtonLink } from '../components/ui/Button';
import { SaveTemplateDialog } from '../components/workout/SaveTemplateDialog';
import { Card } from '../components/ui/Card';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { StatTile } from '../components/ui/StatTile';
import { useMediaQuery } from '../hooks/useMediaQuery';
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
  const phone = useMediaQuery('(max-width: 640px)');
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  // "Повторить": новая тренировка с теми же упражнениями — сразу на экран тренировки (как старт из комплекса)
  const repeat = useMutation({
    mutationFn: () => workoutsApi.repeat(workoutId),
    onSuccess: (next) => {
      queryClient.setQueryData(CURRENT_WORKOUT_KEY, next);
      navigate('/workout/active');
    },
  });

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

  const title =
    mode === 'finished'
      ? 'Тренировка завершена'
      : capitalize(dayTitle.format(new Date(w.startedAt ?? w.completedAt ?? '')));
  const repeatButton = (
    <Button onClick={() => repeat.mutate()} disabled={repeat.isPending}>
      Повторить
    </Button>
  );

  return (
    <div className={styles.wide}>
      <PageHeader
        back={mode === 'view' ? { to: '/history', label: 'История' } : undefined}
        title={title}
        subtitle={`${w.name} · ${formatDuration(w.durationSec ?? 0)}`}
        action={mode === 'finished' ? <ButtonLink to="/">На главную</ButtonLink> : repeatButton}
      />

      {/* плитки — 3 в ряд и на телефоне (компактные), первая — главная цифра тренировки */}
      <div className={`${styles.summaryTiles} ${shown.length > 3 ? styles.summaryTiles4 : ''}`}>
        {shown.map((tile, index) => (
          <StatTile
            key={tile.label}
            compact={phone}
            variant={index === 0 ? 'inverse' : 'default'}
            label={tile.label}
            value={tile.value}
          />
        ))}
      </div>

      {repeat.isError && <p className={styles.muted}>{repeat.error.message}</p>}
      {/* 409 — уже идёт другая тренировка: предлагаем вернуться к ней */}
      {repeat.error instanceof ApiError && repeat.error.status === 409 && (
        <ButtonLink to="/workout/active" variant="secondary">
          Продолжить текущую
        </ButtonLink>
      )}

      <section className={styles.summarySection}>
        <SectionLabel>Упражнения</SectionLabel>
        <div className={styles.summaryGrid}>
          {done.map((exercise) => (
            <Card key={exercise.id} as="article">
              <h3 className={styles.cardTitle}>{exercise.exerciseName}</h3>
              <ol className={styles.setList}>
                {exercise.sets.map((set) => (
                  <li key={set.id} className={styles.summaryExercise}>
                    <span className={styles.muted}>
                      {exercise.trackingType === 'DISTANCE' && exercise.sets.length === 1
                        ? 'Пробежка'
                        : `${set.number}`}
                      {set.warmup && ' · разминка'}
                    </span>
                    <span className={styles.setValueSmall}>
                      {describeSet(exercise.trackingType, set, exercise.assisted)}
                    </span>
                  </li>
                ))}
              </ol>
            </Card>
          ))}
        </div>
      </section>

      {mode === 'finished' && (
        <p className="footnote">Рекорды и прогресс обновятся на главной через несколько секунд.</p>
      )}
      <div className={styles.summaryActions}>
        {!w.templateId && (
          <Button variant="secondary" onClick={() => setSaving(true)}>
            Сохранить как комплекс
          </Button>
        )}
      </div>
      {saving && <SaveTemplateDialog exerciseIds={done.map((e) => e.exerciseId)} onClose={() => setSaving(false)} />}
    </div>
  );
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
