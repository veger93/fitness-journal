import { useEffect, type RefObject } from 'react';

/** Скорость бегущей строки: пикселей в секунду */
const SPEED_PX_PER_S = 80;

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/**
 * Бегущая строка без "дыр" на iPhone.
 *
 * Раньше двигалась вся лента целиком (CSS-анимация на одном элементе шириной ~5000 px). Браузер рисует
 * анимируемый элемент отдельным слоем-картинкой, а на телефоне с плотным экраном это 5000 × 3 = 15 000 точек:
 * больше, чем Safari готов держать в памяти видеокарты одним куском. Он рисует слой кусками ("тайлами"),
 * не успевает — и части слов пропадают.
 *
 * Теперь двигается каждый пункт отдельно, но на одно и то же расстояние (половина ленты — там начинается
 * вторая, такая же половина) и с общим стартом: выглядит как одна лента, а слои маленькие — по ширине пункта.
 * Расстояние меряем после загрузки шрифта и пересчитываем при смене ширины.
 */
export function useMarquee(trackRef: RefObject<HTMLElement | null>) {
  useEffect(() => {
    const track = trackRef.current;
    if (!track || typeof track.animate !== 'function' || prefersReducedMotion()) return;

    let animations: Animation[] = [];
    const start = () => {
      animations.forEach((a) => a.cancel());
      const half = track.scrollWidth / 2;
      if (half <= 0) return;
      const duration = (half / SPEED_PX_PER_S) * 1000;
      // все анимации создаются в одном кадре — у них общий старт, пункты едут вплотную, как одна лента
      animations = Array.from(track.children).map((item) =>
        item.animate([{ transform: 'translateX(0)' }, { transform: `translateX(${-half}px)` }], {
          duration,
          iterations: Infinity,
          easing: 'linear',
        }),
      );
    };

    let cancelled = false;
    // ширина ленты зависит от шрифта: меряем, когда он загрузился
    (document.fonts?.ready ?? Promise.resolve()).then(() => {
      if (!cancelled) start();
    });
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(() => start());
    observer?.observe(track);

    return () => {
      cancelled = true;
      observer?.disconnect();
      animations.forEach((a) => a.cancel());
    };
  }, [trackRef]);
}
