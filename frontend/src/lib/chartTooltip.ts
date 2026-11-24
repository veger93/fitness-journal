import type { CSSProperties } from 'react';

/**
 * Одно оформление всплывающей подсказки графиков (Recharts Tooltip) — во всех стилях и темах.
 * Цвет строки значения задаём явно: без itemStyle Recharts красит её цветом столбика или линии
 * и по умолчанию тёмным — на тёмной подложке («Метро», тёмная тема) текст пропадал.
 */
export const CHART_TOOLTIP: {
  contentStyle: CSSProperties;
  labelStyle: CSSProperties;
  itemStyle: CSSProperties;
  separator: string;
} = {
  contentStyle: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 12,
    color: 'var(--text)',
  },
  labelStyle: { color: 'var(--text-muted)', marginBottom: 2 },
  itemStyle: { color: 'var(--text)', fontWeight: 600, padding: 0 },
  separator: ': ',
};
