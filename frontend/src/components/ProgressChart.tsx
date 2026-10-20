import { CartesianGrid, Line, LineChart, ReferenceArea, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { ProgressPoint } from '../api/analytics';
import { formatKg, formatShortDate } from '../lib/format';

interface ProgressChartProps {
  points: ProgressPoint[];
  /** начало плато — с этой даты закрашиваем зону */
  plateauFrom?: string | null;
}

/**
 * График силы: линия "ты" и пунктир "норма для твоего уровня".
 * Цвета — из токенов темы (var(--...)): SVG понимает CSS-переменные, поэтому график сам перекрашивается.
 * Ось X — числовая (время в мс): так точки стоят на реальном расстоянии по датам, а не через равные шаги.
 */
export function ProgressChart({ points, plateauFrom }: ProgressChartProps) {
  const data = points
    .filter((p) => p.e1rmKg != null)
    .map((p) => ({ time: new Date(p.date).getTime(), you: p.e1rmKg, reference: p.referenceE1rmKg }));

  if (data.length < 2) {
    return <p className="footnote">Нужно хотя бы две тренировки, чтобы построить график.</p>;
  }

  const lastTime = data[data.length - 1].time;
  const plateauStart = plateauFrom ? new Date(plateauFrom).getTime() : null;

  return (
    <div role="img" aria-label="График силы по тренировкам: твоя линия и норма для твоего уровня" style={{ width: '100%', height: 260 }}>
      <ResponsiveContainer>
        <LineChart data={data} margin={{ top: 24, right: 8, bottom: 0, left: 0 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          {plateauStart != null && (
            // подпись зоны — в легенде над графиком: на узком экране внутри зоны она не помещается
            <ReferenceArea x1={plateauStart} x2={lastTime} fill="var(--chip)" fillOpacity={0.6} />
          )}
          <XAxis
            dataKey="time"
            type="number"
            scale="time"
            domain={['dataMin', 'dataMax']}
            tickFormatter={formatShortDate}
            tick={{ fill: 'var(--text-muted)', fontSize: 12 }}
            axisLine={false}
            tickLine={false}
            minTickGap={24}
          />
          <YAxis
            width={44}
            domain={['auto', 'auto']}
            tick={{ fill: 'var(--text-muted)', fontSize: 12 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            labelFormatter={(time) => formatShortDate(Number(time))}
            formatter={(value, name) => [formatKg(Number(value)), name === 'you' ? 'ты' : 'норма']}
            contentStyle={{
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 12,
              color: 'var(--text)',
            }}
          />
          <Line
            dataKey="reference"
            stroke="var(--text-muted)"
            strokeWidth={2}
            strokeDasharray="5 5"
            dot={false}
            isAnimationActive={false}
          />
          <Line
            dataKey="you"
            stroke="var(--accent-strong)"
            strokeWidth={4}
            strokeLinecap="round"
            dot={false}
            activeDot={{ r: 6 }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
