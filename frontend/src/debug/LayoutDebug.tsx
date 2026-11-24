import { useEffect, useRef, useState } from 'react';
import { formatSnapshot, isBroken, takeSnapshot, type LayoutSnapshot } from './layoutProbe';
import styles from './LayoutDebug.module.css';

/**
 * Панель «Диагностика экрана» (Профиль → Оформление). ВРЕМЕННО: ищем, почему на iPhone страница
 * иногда сжимается или съезжает вбок. Следит за шириной и масштабом и в момент поломки
 * запоминает снимок: что было в фокусе и какие элементы вылезли за край. Снимок можно скопировать.
 */
export function LayoutDebug() {
  const [now, setNow] = useState<LayoutSnapshot>(() => takeSnapshot('старт'));
  const [caught, setCaught] = useState<LayoutSnapshot[]>([]);
  const [open, setOpen] = useState(true);
  const [copied, setCopied] = useState(false);
  const wasBroken = useRef(false);
  const lastEvent = useRef('старт');

  useEffect(() => {
    const check = (event: string) => {
      const snap = takeSnapshot(event);
      setNow(snap);
      const broken = isBroken(snap);
      // ловим именно момент поломки (переход «было нормально → сломалось»), вместе с прошлым событием
      if (broken && !wasBroken.current) {
        setCaught((list) => [{ ...snap, event: `${event} (до этого: ${lastEvent.current})` }, ...list].slice(0, 5));
      }
      wasBroken.current = broken;
      lastEvent.current = event;
    };
    const on = (type: string) => () => check(type);
    const handlers: [EventTarget, string, () => void][] = [
      [window, 'resize', on('resize')],
      [window, 'scroll', on('scroll')],
      [window, 'orientationchange', on('поворот')],
      [document, 'focusin', on('фокус (клавиатура)')],
      [document, 'focusout', on('уход фокуса')],
      [document, 'click', on('нажатие')],
      [window, 'popstate', on('переход')],
    ];
    if (window.visualViewport) {
      handlers.push([window.visualViewport, 'resize', on('видимая область: размер')]);
      handlers.push([window.visualViewport, 'scroll', on('видимая область: сдвиг')]);
    }
    handlers.forEach(([target, type, fn]) => target.addEventListener(type, fn, { passive: true }));
    // часть изменений приходит без событий (анимации, догрузка данных) — раз в полсекунды проверяем сами
    const timer = window.setInterval(() => check('таймер'), 500);
    return () => {
      handlers.forEach(([target, type, fn]) => target.removeEventListener(type, fn));
      window.clearInterval(timer);
    };
  }, []);

  const copy = async () => {
    const text = [
      `Устройство: ${navigator.userAgent}`,
      `Как открыто: ${matchMedia('(display-mode: standalone)').matches ? 'с экрана «Домой»' : 'в браузере'}`,
      '— сейчас —',
      formatSnapshot(now),
      ...caught.map((s, i) => `— поломка ${i + 1} —\n${formatSnapshot(s)}`),
    ].join('\n');
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      window.prompt('Скопируйте текст', text);
    }
  };

  const broken = isBroken(now);
  if (!open) {
    return (
      <button type="button" className={`${styles.badge} ${broken ? styles.bad : ''}`} onClick={() => setOpen(true)}>
        {broken ? '⚠' : '✓'} {caught.length}
      </button>
    );
  }
  return (
    <div className={`${styles.panel} ${broken ? styles.bad : ''}`} role="status">
      <div className={styles.row}>
        <b>{broken ? '⚠ Сломалось' : '✓ Норма'}</b>
        <span>поймано: {caught.length}</span>
        <button type="button" onClick={() => void copy()}>
          {copied ? 'Скопировано' : 'Скопировать'}
        </button>
        <button type="button" onClick={() => setOpen(false)} aria-label="Свернуть">
          –
        </button>
      </div>
      <pre className={styles.text}>{formatSnapshot(caught[0] ?? now)}</pre>
    </div>
  );
}
