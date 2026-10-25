import { useRef, useState, type PointerEvent } from 'react';
import type { ExerciseSummary } from '../../api/analytics';
import { Link } from 'react-router-dom';
import { useCarousel } from '../../hooks/useCarousel';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import { ExerciseRow } from '../ExerciseRow';
import styles from './Home.module.css';

const SWIPE_PX = 40;

/**
 * Все упражнения — по одной строке, листаются влево сами каждые 4 секунды.
 * Пауза: наведение мышью, фокус с клавиатуры и системная настройка "уменьшить движение".
 * На телефоне — свайп пальцем.
 */
export function ExerciseTicker({ exercises }: { exercises: ExerciseSummary[] }) {
  const reducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const count = exercises.length;
  const carousel = useCarousel(count, { paused: hovered || focused || reducedMotion });
  const swipeStart = useRef<number | null>(null);
  const swiped = useRef(false);

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
    // свайп по строке-ссылке не должен открывать страницу — запоминаем и гасим ближайший клик
    swiped.current = Math.abs(dx) > SWIPE_PX;
    if (dx < -SWIPE_PX) {
      carousel.next();
    } else if (dx > SWIPE_PX) {
      carousel.prev();
    }
  };

  return (
    <section className={styles.section} aria-labelledby="exercises-title">
      <div className={styles.sectionHead}>
        <h2 id="exercises-title" className={styles.sectionTitle}>
          Упражнения
        </h2>
        <Link to="/exercises" className={styles.sectionLink}>
          Все упражнения →
        </Link>
      </div>
      <div
        className={styles.ticker}
        aria-roledescription="карусель"
        aria-label="Упражнения по одному"
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
      >
        <div
          className={styles.viewport}
          onPointerDown={onPointerDown}
          onPointerUp={onPointerUp}
          onClickCapture={(e) => {
            if (swiped.current) {
              e.preventDefault();
              swiped.current = false;
            }
          }}
        >
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
                <ExerciseRow exercise={exercise} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
