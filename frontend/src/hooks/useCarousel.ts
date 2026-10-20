import { useCallback, useEffect, useState } from 'react';

/**
 * Карусель, которая всегда листает ВЛЕВО и бесконечно.
 *
 * Как сделан бесконечный круг: в конец ленты добавляем копию первого слайда.
 * Доехали до копии (position === count) — после анимации без анимации прыгаем на настоящий первый (0).
 * Для глаза это незаметно: копия и оригинал выглядят одинаково.
 *
 * position — сколько слайдов "уехало" влево (0..count), active — какой слайд реально показан (0..count-1).
 */
export function useCarousel(count: number, { intervalMs = 4000, paused = false } = {}) {
  const [position, setPosition] = useState(0);
  const [animate, setAnimate] = useState(true);

  // автопрокрутка
  useEffect(() => {
    if (paused || count < 2) {
      return;
    }
    const id = window.setInterval(() => {
      setAnimate(true);
      setPosition((p) => p + 1);
    }, intervalMs);
    return () => window.clearInterval(id);
  }, [paused, count, intervalMs]);

  // список поменялся (обновились данные) — не оставляем позицию за пределами
  useEffect(() => {
    setPosition((p) => (p > count ? 0 : p));
  }, [count]);

  // после "прыжка" без анимации возвращаем анимацию со следующего кадра,
  // иначе браузер склеит оба изменения и прыжок станет видимым
  useEffect(() => {
    if (animate) {
      return;
    }
    const id = requestAnimationFrame(() => requestAnimationFrame(() => setAnimate(true)));
    return () => cancelAnimationFrame(id);
  }, [animate]);

  const onTransitionEnd = useCallback(() => {
    if (position >= count) {
      setAnimate(false);
      setPosition(0);
    }
  }, [position, count]);

  const goTo = useCallback((index: number) => {
    setAnimate(true);
    setPosition(index);
  }, []);

  const next = useCallback(() => goTo(position + 1), [goTo, position]);

  const prev = useCallback(() => {
    if (position === 0) {
      setAnimate(false); // с первого назад — сразу на последний, без "перемотки" через всю ленту
      setPosition(count - 1);
    } else {
      goTo(position - 1);
    }
  }, [goTo, position, count]);

  return { position, active: count === 0 ? 0 : position % count, animate, goTo, next, prev, onTransitionEnd };
}
