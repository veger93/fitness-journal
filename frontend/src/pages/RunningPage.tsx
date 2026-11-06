import { useQuery } from '@tanstack/react-query';
import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { analyticsApi, type RunningSummary } from '../api/analytics';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { Card } from '../components/ui/Card';
import { StatTile } from '../components/ui/StatTile';
import { formatChange, formatShortDate } from '../lib/format';
import { formatClock, formatDistance } from '../lib/workout';
import styles from './RunningPage.module.css';

/** Скорость из темпа: 290 с/км -> "12,4 км/ч" */
function speedText(secPerKm: number): string {
  return `${new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 }).format(3600 / secPerKm)} км/ч`;
}

/** Темп из секунд на км: 290 -> "4:50 /км · 12,4 км/ч" (на телефоне в плитке — только скорость, темп и так крупно) */
function paceText(secPerKm: number): string {
  return `${formatClock(secPerKm)} /км · ${speedText(secPerKm)}`;
}

/** "Бег": километраж по неделям, рост нагрузки, рекорды, баланс лёгких и тяжёлых пробежек. */
export function RunningPage() {
  const running = useQuery({ queryKey: ['running'], queryFn: analyticsApi.running });

  return (
    <div className={styles.page}>
      <PageHeader back={{ to: '/exercises', label: 'Все упражнения' }} title="Бег" />
      {running.isPending && <p className={styles.muted}>Загружаем…</p>}
      {running.isError && <p className={styles.muted}>{running.error.message}</p>}
      {running.data?.empty && (
        <Card variant="inverse" size="section">
          <h2 className={styles.cardTitle}>Пробежек пока нет</h2>
          <p className={styles.onInverse}>
            Добавьте в тренировку упражнение «Бег» — здесь появятся километраж, рекорды и подсказки.
          </p>
        </Card>
      )}
      {running.data && !running.data.empty && <Content data={running.data} />}
    </div>
  );
}

function Content({ data }: { data: RunningSummary }) {
  const thisWeek = data.weeks.at(-1)?.distanceM ?? 0;
  const { load, effort } = data;
  const rated = effort.easy + effort.moderate + effort.hard;
  // телефон: три плитки в ряд, компактные (как на главной)
  const compact = useMediaQuery('(max-width: 640px)');

  return (
    <>
      {load.warning && load.hint && (
        <Card variant="accent">
          <h2 className={styles.cardTitle}>Притормозите с километражем</h2>
          <p className={styles.plain}>{load.hint}*</p>
        </Card>
      )}

      <div className={styles.tiles}>
        <StatTile
          compact={compact}
          variant="inverse"
          label="На этой неделе"
          value={thisWeek > 0 ? formatDistance(thisWeek) : '0 км'}
        />
        {data.longestRun?.distanceM != null && (
          <StatTile
            compact={compact}
            label="Самая длинная"
            value={formatDistance(data.longestRun.distanceM)}
            caption={`${formatShortDate(data.longestRun.date)}${data.longestRun.fresh ? (compact ? ' · рекорд' : ' · новый рекорд') : ''}`}
            highlightCaption={data.longestRun.fresh}
          />
        )}
        {data.bestPace?.paceSecPerKm != null && (
          <StatTile
            compact={compact}
            label="Лучший темп**"
            value={formatClock(data.bestPace.paceSecPerKm)}
            caption={`${compact ? speedText(data.bestPace.paceSecPerKm) : paceText(data.bestPace.paceSecPerKm)}${data.bestPace.fresh ? ' · рекорд' : ''}`}
            highlightCaption={data.bestPace.fresh}
          />
        )}
      </div>

      {/* ПК: километраж (ему нужна ширина) | как давались пробежки и сноски */}
      <PageColumns
        aside={
          <>
            <EffortCard effort={effort} rated={rated} />
            <div className={styles.footnotes}>
              {load.warning && (
                <p className="footnote">
                  * Исследование начинающих бегунов: при росте больше 30% за две недели травм от перегрузки было больше.
                  Связь не строгая, но прибавлять плавнее — безопаснее.
                </p>
              )}
              <p className="footnote">** Темп — сколько минут уходит на 1 км. Считаем по отрезкам от 1 км.</p>
            </div>
          </>
        }
      >
        <Card as="section" size="section">
          <h2 className={styles.cardTitle}>Километраж по неделям</h2>
          <WeeksChart weeks={data.weeks} />
          <p className={styles.muted}>
            За 2 недели: {formatDistance(load.lastTwoWeeksM)}
            {load.changePercent != null && ` (${formatChange(load.changePercent)} к двум предыдущим)`}
          </p>
        </Card>
      </PageColumns>
    </>
  );
}

function EffortCard({ effort, rated }: { effort: RunningSummary['effort']; rated: number }) {
  return (
    <Card as="section">
      <h2 className={styles.cardTitle}>Как давались пробежки</h2>
      <p className={styles.muted}>За последние 4 недели, по отметке «Как далась пробежка?»</p>
      {rated > 0 ? (
        <>
          <div className={styles.effortBar} aria-hidden="true">
            <span className={styles.easy} style={{ flexGrow: effort.easy }} />
            <span className={styles.moderate} style={{ flexGrow: effort.moderate }} />
            <span className={styles.hard} style={{ flexGrow: effort.hard }} />
          </div>
          <ul className={styles.effortLegend}>
            <li>
              <span className={`${styles.dot} ${styles.easy}`} aria-hidden="true" />
              Легко: {effort.easy}
            </li>
            <li>
              <span className={`${styles.dot} ${styles.moderate}`} aria-hidden="true" />
              Средне: {effort.moderate}
            </li>
            <li>
              <span className={`${styles.dot} ${styles.hard}`} aria-hidden="true" />
              Тяжело: {effort.hard}
            </li>
          </ul>
        </>
      ) : (
        <p className={styles.plain}>
          Отмечайте после пробежки, как она далась, — здесь появится баланс лёгких и тяжёлых.
        </p>
      )}
      {effort.unrated > 0 && rated > 0 && <p className={styles.muted}>Без отметки: {effort.unrated}</p>}
      {effort.hint && <p className={styles.plain}>{effort.hint}</p>}
    </Card>
  );
}

function WeeksChart({ weeks }: { weeks: RunningSummary['weeks'] }) {
  const data = weeks.map((w, i) => ({
    label: formatShortDate(w.weekStart),
    km: Math.round(w.distanceM / 100) / 10,
    current: i === weeks.length - 1,
  }));
  return (
    <div role="img" aria-label="Километраж по неделям за 8 недель" style={{ width: '100%', height: 220 }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 0, bottom: 0, left: 0 }}>
          <XAxis dataKey="label" tick={{ fill: 'var(--text-muted)', fontSize: 12 }} axisLine={false} tickLine={false} />
          <YAxis width={32} tick={{ fill: 'var(--text-muted)', fontSize: 12 }} axisLine={false} tickLine={false} />
          <Tooltip
            cursor={{ fill: 'var(--chip)', opacity: 0.4 }}
            formatter={(value) => [`${value} км`, 'пробежано']}
            labelFormatter={(label) => `неделя с ${label}`}
            contentStyle={{
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 12,
              color: 'var(--text)',
            }}
          />
          <Bar dataKey="km" radius={[8, 8, 0, 0]} isAnimationActive={false}>
            {/* текущая неделя — акцентом, прошлые — спокойным цветом */}
            {data.map((d) => (
              <Cell key={d.label} fill={d.current ? 'var(--accent-strong)' : 'var(--text-muted)'} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
