import { useLayoutEffect, useRef, type RefObject } from 'react';
import { CAPSULE_DURATION, capsuleFrame, type Span } from '../lib/capsule';

/** Активная вкладка: NavLink сам ставит ей aria-current="page" */
function activeSpan(nav: HTMLElement): Span | null {
  const tab = nav.querySelector<HTMLElement>('[aria-current="page"]');
  return tab ? { left: tab.offsetLeft, right: tab.offsetLeft + tab.offsetWidth } : null;
}

function draw(capsule: HTMLElement, span: Span | null, squash = 0) {
  if (!span) {
    capsule.style.opacity = '0'; // раздел вне панели (например, страница упражнения) — капсулы нет
    return;
  }
  capsule.style.opacity = '1';
  capsule.style.width = `${span.right - span.left}px`;
  capsule.style.transform = `translateX(${span.left}px) scaleY(${1 - squash})`;
}

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/**
 * Двигает капсулу за активной вкладкой при смене раздела (key — путь страницы).
 *
 * Почему JS-анимация, а не CSS transition: подпись у вкладки тоже плавно раскрывается, и вкладки во время
 * перехода меняют ширину и сдвигаются. Поэтому каждый кадр заново меряем, где сейчас цель, и ведём капсулу к ней —
 * в конце она точно совпадает с вкладкой.
 */
export function useCapsule(
  navRef: RefObject<HTMLElement | null>,
  capsuleRef: RefObject<HTMLElement | null>,
  key: string,
) {
  // где капсула нарисована сейчас; null — ещё не рисовали
  const current = useRef<Span | null>(null);
  // идёт "перетекание": пока оно идёт, капсулу ведёт анимация, а не наблюдатель размеров
  const animating = useRef(false);

  useLayoutEffect(() => {
    const nav = navRef.current;
    const capsule = capsuleRef.current;
    if (!nav || !capsule) return;

    const from = current.current;
    const target = activeSpan(nav);
    // первый показ, переход из раздела без вкладки или "уменьшить движение" в настройках — без анимации
    if (!from || !target || prefersReducedMotion()) {
      draw(capsule, target);
      current.current = target;
      return;
    }

    const start = performance.now();
    let frame = 0;
    animating.current = true;
    const step = (now: number) => {
      const to = activeSpan(nav) ?? target;
      const t = Math.min((now - start) / CAPSULE_DURATION, 1);
      const { left, right, squash } = capsuleFrame(from, to, t);
      draw(capsule, { left, right }, squash);
      current.current = { left, right };
      if (t < 1) {
        frame = requestAnimationFrame(step);
      } else {
        animating.current = false;
      }
    };
    frame = requestAnimationFrame(step);
    // ушли на другую вкладку посреди перехода — новая анимация стартует с того места, где капсула сейчас
    return () => {
      cancelAnimationFrame(frame);
      animating.current = false;
    };
  }, [navRef, capsuleRef, key]);

  /*
   * Поворот экрана, смена ширины окна — и, главное, вкладки, которые меняют ширину (подпись раскрывается
   * CSS-переходом 0,35 с). Если телефон занят (отрисовка новой страницы), кадры анимации пропускаются,
   * и "перетекание" может закончиться раньше, чем подпись раскрылась: капсула застыла бы кружком,
   * а текст вылез бы за неё. Поэтому следим и за размером каждой вкладки и дотягиваем капсулу.
   */
  useLayoutEffect(() => {
    const nav = navRef.current;
    const capsule = capsuleRef.current;
    if (!nav || !capsule || typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(() => {
      if (animating.current) return; // анимация сама меряет цель на каждом кадре
      const span = activeSpan(nav);
      draw(capsule, span);
      current.current = span;
    });
    observer.observe(nav);
    nav.querySelectorAll('a').forEach((tab) => observer.observe(tab));
    return () => observer.disconnect();
  }, [navRef, capsuleRef]);
}
