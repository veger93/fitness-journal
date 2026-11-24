/**
 * Диагностика «страница съезжает вбок / сжимается» (iPhone). ВРЕМЕННО: удалить, когда баг найдём.
 * Чистые функции — без React, их проверяют тесты (layoutProbe.test.ts).
 */

export interface Offender {
  /** «div.ui-card _card_x7f3a» — тег, классы (CSS-модули в сборке: _имя_хеш) */
  name: string;
  left: number;
  right: number;
  width: number;
}

export interface LayoutSnapshot {
  time: string;
  path: string;
  event: string;
  innerWidth: number;
  clientWidth: number;
  scrollWidth: number;
  bodyScrollWidth: number;
  scrollX: number;
  vvWidth: number | null;
  vvScale: number | null;
  vvOffsetLeft: number | null;
  /** элемент в фокусе: поле ввода открывает клавиатуру */
  focused: string;
  offenders: Offender[];
}

/**
 * Признаки поломки: страница шире экрана, сдвинута вбок, масштаб не 1 или что-то вылезло за край
 * (у html стоит overflow-x: clip — ширина содержимого тогда не растёт, но сам элемент за краем видно).
 */
export function isBroken(
  s: Pick<LayoutSnapshot, 'clientWidth' | 'scrollWidth' | 'scrollX' | 'vvScale' | 'vvOffsetLeft'> & {
    offenders?: Offender[];
  },
) {
  return (
    (s.offenders?.length ?? 0) > 0 ||
    s.scrollWidth > s.clientWidth + 1 ||
    Math.abs(s.scrollX) > 1 ||
    (s.vvScale !== null && Math.abs(s.vvScale - 1) > 0.01) ||
    (s.vvOffsetLeft !== null && Math.abs(s.vvOffsetLeft) > 1)
  );
}

export function describeElement(el: Element): string {
  const classes = typeof el.className === 'string' ? el.className.trim().split(/\s+/).slice(0, 3).join(' ') : '';
  return classes ? `${el.tagName.toLowerCase()}.${classes.replace(/ /g, '.')}` : el.tagName.toLowerCase();
}

/** Элементы, которые выходят за край экрана (самые широкие — первыми). Внутри лент с прокруткой не считаем — это норма. */
export function findOffenders(root: Element, viewportWidth: number, limit = 6): Offender[] {
  const found: Offender[] = [];
  for (const el of Array.from(root.querySelectorAll('*'))) {
    const r = el.getBoundingClientRect();
    if (r.width === 0 && r.height === 0) continue;
    if (r.right <= viewportWidth + 1 && r.left >= -1) continue;
    if (insideScroller(el)) continue;
    found.push({
      name: describeElement(el),
      left: Math.round(r.left),
      right: Math.round(r.right),
      width: Math.round(r.width),
    });
  }
  return found.sort((a, b) => b.right - a.right).slice(0, limit);
}

function insideScroller(el: Element): boolean {
  for (let p = el.parentElement; p && p !== document.body; p = p.parentElement) {
    const o = getComputedStyle(p).overflowX;
    if (o === 'auto' || o === 'scroll' || o === 'hidden' || o === 'clip') return true;
  }
  return false;
}

export function takeSnapshot(event: string): LayoutSnapshot {
  const vv = window.visualViewport;
  const doc = document.documentElement;
  const active = document.activeElement;
  return {
    time: new Date().toLocaleTimeString('ru-RU'),
    path: location.pathname,
    event,
    innerWidth: window.innerWidth,
    clientWidth: doc.clientWidth,
    scrollWidth: doc.scrollWidth,
    bodyScrollWidth: document.body.scrollWidth,
    scrollX: Math.round(window.scrollX),
    vvWidth: vv ? Math.round(vv.width) : null,
    vvScale: vv ? Math.round(vv.scale * 100) / 100 : null,
    vvOffsetLeft: vv ? Math.round(vv.offsetLeft) : null,
    focused: active && active !== document.body ? describeElement(active) : '—',
    offenders: findOffenders(document.body, doc.clientWidth),
  };
}

/** Текст для «Скопировать» — его присылают разработчику */
export function formatSnapshot(s: LayoutSnapshot): string {
  const lines = [
    `${s.time} ${s.path} · событие: ${s.event}`,
    `окно ${s.innerWidth} · страница ${s.clientWidth} · ширина содержимого ${s.scrollWidth} (body ${s.bodyScrollWidth}) · сдвиг ${s.scrollX}`,
    `видимая область: ширина ${s.vvWidth ?? '?'} · масштаб ${s.vvScale ?? '?'} · сдвиг ${s.vvOffsetLeft ?? '?'}`,
    `фокус: ${s.focused}`,
    s.offenders.length
      ? 'за краем:\n' + s.offenders.map((o) => `  ${o.name} [${o.left}…${o.right}, ш ${o.width}]`).join('\n')
      : 'за краем: ничего',
  ];
  return lines.join('\n');
}
