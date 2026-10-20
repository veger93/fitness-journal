import { useRef, useState, type PointerEvent } from 'react';
import type { ExerciseStatus, ExerciseSummary } from '../../api/analytics';
import { formatSet, formatShortDate } from '../../lib/format';
import { useCarousel } from '../../hooks/useCarousel';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import { Badge } from '../ui/Badge';
import { Sparkline } from '../ui/Sparkline';
import styles from './Home.module.css';

const BADGES: Record<
  Exclude<ExerciseStatus, 'NORMAL'>,
  { label: string; variant: 'neutral' | 'inverse' | 'accent' }
> = {
  OVERLOAD: { label: 'перегрузка', variant: 'accent' },
  PLATEAU: { label: 'плато', variant: 'neutral' },
  RECORD: { label: 'рекорд', variant: 'inverse' },
};

const SWIPE_PX = 40;

/**
 * Все упражнения — по одной строке, листаются влево сами каждые 4 секунды.
 * Пауза: наведение мышью, фокус с клавиатуры, кнопка ⏸ и системная настройка "уменьшить движение".
 * На телефоне — свайп пальцем.
 */
export function ExerciseTicker({ exercises }: { exercises: ExerciseSummary[] }) {
  const reducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
  const [userPaused, setUserPaused] = useState(reducedMotion);
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const count = exercises.length;
  const carousel = useCarousel(count, { paused: userPaused || hovered || focused || reducedMotion });
  const swipeStart = useRef<number | null>(null);

  if (count === 0) {
    return null;
  }

  // копия первого слайда в конце — для бесконечного круга (см. useCarousel)
  const slides = count > 1 ? [...exercises, exercises[0]] : exercises;

  const onPointerDown = (e: PointerEvent) => {
    swipeStart.current = e.clientX;
  };
  const onPointerUp = (e: PointerEvent) => {
    if (swipeStart.current === null) {
      return;
    }
    const dx = e.clientX - swipeStart.current;
    swipeStart.current = null;
    if (dx < -SWIPE_PX) {
      carousel.next();
    } else if (dx > SWIPE_PX) {
      carousel.prev();
    }
  };

  return (
    <section
      className={styles.ticker}
      aria-roledescription="карусель"
      aria-label="Все упражнения"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
    >
      <div className={styles.viewport} onPointerDown={onPointerDown} onPointerUp={onPointerUp}>
        <div
          className={`${styles.track} ${carousel.animate && !reducedMotion ? styles.animated : ''}`}
          style={{ transform: `translateX(-${carousel.position * 100}%)` }}
          onTransitionEnd={carousel.onTransitionEnd}
        >
          {slides.map((exercise, index) => (
            <div
              key={`${exercise.exerciseId}-${index}`}
              className={styles.slide}
              // невидимые слайды недоступны для Tab и дикторов
              inert={index !== carousel.position}
              aria-roledescription="слайд"
              aria-label={`${(index % count) + 1} из ${count}`}
            >
              <TickerRow exercise={exercise} />
            </div>
          ))}
        </div>
      </div>

      {count > 1 && (
        <div className={styles.controls}>
          <div className={styles.dots}>
            {exercises.map((exercise, index) => (
              <button
                key={exercise.exerciseId}
                type="button"
                className={`${styles.dot} ${index === carousel.active ? styles.dotActive : ''}`}
                aria-label={`Показать: ${exercise.exerciseName}`}
                aria-current={index === carousel.active}
                onClick={() => carousel.goTo(index)}
              />
            ))}
          </div>
          <button
            type="button"
            className={styles.pause}
            aria-label={userPaused ? 'Листать автоматически' : 'Остановить автопрокрутку'}
            onClick={() => setUserPaused((p) => !p)}
          >
            <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              {userPaused ? <path d="M8 5v14l11-7z" /> : <path d="M7 5h4v14H7zM13 5h4v14h-4z" />}
            </svg>
          </button>
        </div>
      )}
    </section>
  );
}

function TickerRow({ exercise }: { exercise: ExerciseSummary }) {
  const badge = exercise.status === 'NORMAL' ? null : BADGES[exercise.status];
  return (
    <div className={styles.row}>
      <div className={styles.rowName}>
        <span className={styles.rowTitle}>{exercise.exerciseName}</span>
        <span className={styles.rowNote}>
          {exercise.note ?? `последняя тренировка ${formatShortDate(exercise.lastPerformedAt)}`}
        </span>
      </div>
      <span className={styles.rowSet}>{formatSet(exercise.lastWeightKg, exercise.lastReps) ?? '—'}</span>
      <span className={styles.rowSpark}>
        <Sparkline values={exercise.trend} accent={exercise.status === 'OVERLOAD'} />
      </span>
      <span className={styles.rowBadge}>{badge && <Badge variant={badge.variant}>{badge.label}</Badge>}</span>
    </div>
  );
}
