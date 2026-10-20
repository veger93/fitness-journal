/** Точки для <polyline>: значения вписываем в рамку width×height с отступом pad. */
export function sparklinePoints(values: number[], width: number, height: number, pad = 3): string {
  if (values.length < 2) {
    return '';
  }
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1; // все значения равны — рисуем ровную линию посередине
  const stepX = (width - pad * 2) / (values.length - 1);
  return values
    .map((v, i) => {
      const x = pad + i * stepX;
      const y = max === min ? height / 2 : pad + (1 - (v - min) / range) * (height - pad * 2);
      return `${round(x)},${round(y)}`;
    })
    .join(' ');
}

interface SparklineProps {
  values: number[];
  width?: number;
  height?: number;
  /** подсветить (перегрузка) — иначе спокойный серый */
  accent?: boolean;
}

/** Мини-график без осей: только форма — растёт, стоит или падает. Декоративный, смысл дублирует бейдж. */
export function Sparkline({ values, width = 120, height = 32, accent = false }: SparklineProps) {
  const points = sparklinePoints(values, width, height);
  if (!points) {
    return <span style={{ width }} aria-hidden="true" />;
  }
  return (
    <svg aria-hidden="true" width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
      <polyline
        points={points}
        fill="none"
        stroke={accent ? 'var(--accent-strong)' : 'var(--text-muted)'}
        strokeWidth={2.5}
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  );
}

function round(n: number): number {
  return Math.round(n * 10) / 10;
}
