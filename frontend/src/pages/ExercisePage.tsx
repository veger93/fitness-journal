import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  analyticsApi,
  type ExerciseInsights,
  type ExerciseProgress,
  type ExerciseSummary,
  type ProgressPeriod,
} from '../api/analytics';
import { workoutsApi } from '../api/workouts';
import { DeloadPlanContent } from '../components/home/DeloadPlanContent';
import { PlateauTips } from '../components/home/PlateauTips';
import { STATUS_BADGES } from '../components/ExerciseRow';
import { ProgressChart } from '../components/ProgressChart';
import { ProgressLegend } from '../components/ProgressLegend';
import { BackLink } from '../components/ui/BackLink';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { StatTile } from '../components/ui/StatTile';
import { bestDuration, historyRows } from '../lib/exercise';
import { formatChange, formatExerciseResult, formatKg, formatShortDate, formatTonnage } from '../lib/format';
import { formatClock } from '../lib/workout';
import styles from './ExercisePage.module.css';

const PERIODS: { value: ProgressPeriod; label: string }[] = [
  { value: 'M1', label: 'Месяц' },
  { value: 'M3', label: '3 мес' },
  { value: 'M6', label: 'Полгода' },
  { value: 'Y1', label: 'Год' },
  { value: 'ALL', label: 'Всё' },
];

const PERIOD_CAPTION: Record<ProgressPeriod, string> = {
  M1: 'за месяц',
  M3: 'за 3 месяца',
  M6: 'за полгода',
  Y1: 'за год',
  ALL: 'за всё время',
};

/**
 * Страница одного упражнения: сила и её график за выбранный период, подсказка системы,
 * план разгрузки или советы при плато (если они нужны) и история по тренировкам.
 *
 * Данных три: сводка упражнения берётся из /dashboard (тот же кэш, что у главной — лишнего запроса нет),
 * progress — по выбранному периоду (период входит в queryKey, поэтому каждый период кэшируется отдельно),
 * insights — плато, перегрузка и совет.
 */
export function ExercisePage() {
  const { exerciseId = '' } = useParams();
  const [period, setPeriod] = useState<ProgressPeriod>('M3');

  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: analyticsApi.dashboard });
  const progress = useQuery({
    queryKey: ['progress', exerciseId, period],
    queryFn: () => analyticsApi.progress(exerciseId, period),
  });
  const insights = useQuery({
    queryKey: ['insights', exerciseId],
    queryFn: () => analyticsApi.insights(exerciseId),
  });

  const summary = dashboard.data?.exercises.find((e) => e.exerciseId === exerciseId) ?? null;
  const name = summary?.exerciseName ?? progress.data?.exerciseName ?? insights.data?.exerciseName;

  if (dashboard.isPending) {
    return <p className={styles.muted}>Загружаем…</p>;
  }
  if (dashboard.isError) {
    return <p className={styles.muted}>{dashboard.error.message}</p>;
  }
  if (!summary) {
    return (
      <div className={styles.page}>
        <BackLink to="/exercises">Все упражнения</BackLink>
        <h1 className={styles.title}>{name ?? 'Упражнение'}</h1>
        <p className={styles.muted}>Пока нет завершённых тренировок с этим упражнением.</p>
      </div>
    );
  }

  const badge = summary.status === 'NORMAL' ? null : STATUS_BADGES[summary.status];
  const timed = summary.trackingType === 'TIME';

  return (
    <div className={styles.page}>
      <BackLink to="/exercises">Все упражнения</BackLink>
      <div className={styles.header}>
        <h1 className={styles.title}>{summary.exerciseName}</h1>
        {badge && <Badge variant={badge.variant}>{badge.label}</Badge>}
      </div>

      <SegmentedControl legend="Период" options={PERIODS} value={period} onChange={setPeriod} />

      <Tiles summary={summary} progress={progress.data} insights={insights.data} period={period} />

      {/* у упражнений на время нет веса — нет и силы (1ПМ), график силы для них не строим */}
      {!timed && (
        <Card as="section" size="section">
          <h2 className={styles.cardTitle}>Сила*</h2>
          <ProgressLegend
            plateauDays={insights.data?.plateau.detected ? insights.data.plateau.daysSinceProgress : null}
          />
          {progress.data ? (
            <ProgressChart
              points={progress.data.points}
              plateauFrom={insights.data?.plateau.detected ? insights.data.plateau.lastProgressAt : null}
            />
          ) : (
            <div className={`${styles.muted} ${styles.chartPlaceholder}`}>
              {progress.isError ? progress.error.message : 'Загружаем график…'}
            </div>
          )}
          {progress.data?.reference && <p className={styles.muted}>Темп: {progress.data.reference.hint}.</p>}
        </Card>
      )}

      {insights.data && <Advice summary={summary} insights={insights.data} />}

      <section className={styles.section}>
        <h2 className={styles.cardTitle}>История</h2>
        <p className={styles.muted}>Лучший подход каждой тренировки{timed ? '' : ' и сила* по нему'}.</p>
        <History summary={summary} progress={progress.data} />
      </section>

      {!timed && (
        <p className="footnote">
          * Сила — сколько ты поднял бы один раз, посчитано по лучшему подходу тренировки (формула Эпли).
        </p>
      )}
    </div>
  );
}

function Tiles({
  summary,
  progress,
  insights,
  period,
}: {
  summary: ExerciseSummary;
  progress: ExerciseProgress | undefined;
  insights: ExerciseInsights | undefined;
  period: ProgressPeriod;
}) {
  const last = formatExerciseResult(summary) ?? '—';
  const sessions = insights?.sessionsCount;

  if (summary.trackingType === 'TIME') {
    const best = progress ? bestDuration(progress.points) : null;
    return (
      <div className={styles.tiles}>
        <StatTile
          variant="inverse"
          label="Лучшее время"
          value={best != null ? formatClock(best) : '—'}
          caption={PERIOD_CAPTION[period]}
        />
        <StatTile label="Последний результат" value={last} caption={formatShortDate(summary.lastPerformedAt)} />
        <StatTile label="Тренировок" value={sessions ?? '—'} caption="с этим упражнением" />
      </div>
    );
  }

  const change = progress?.changePercent;
  return (
    <div className={styles.tiles}>
      <StatTile
        variant="inverse"
        label="Сила*"
        value={summary.currentE1rmKg != null ? formatKg(summary.currentE1rmKg) : '—'}
        caption={change != null ? `${formatChange(change)} ${PERIOD_CAPTION[period]}` : 'мало данных за период'}
        highlightCaption={change != null && change > 0}
      />
      <StatTile label="Последний подход" value={last} caption={formatShortDate(summary.lastPerformedAt)} />
      <StatTile
        label="Объём за неделю"
        value={progress?.weeklyVolumeKg != null ? formatTonnage(progress.weeklyVolumeKg) : '—'}
        caption="вес × повторы"
      />
      <StatTile label="Тренировок" value={sessions ?? '—'} caption="с этим упражнением" />
    </div>
  );
}

/**
 * Совет системы и что с ним делать. Перегрузка — сразу план разгрузки (раньше был только в окне на главной),
 * плато — приёмы, которые помогают сдвинуться.
 */
function Advice({ summary, insights }: { summary: ExerciseSummary; insights: ExerciseInsights }) {
  const deloads = useQuery({ queryKey: ['deloads'], queryFn: workoutsApi.deloads });
  const deloading = deloads.data?.some((d) => d.exerciseId === summary.exerciseId) ?? false;
  // идёт разгрузка — блок остаётся, даже если перегрузка уже ушла: из него разгрузку можно закончить
  if (summary.status === 'OVERLOAD' || deloading) {
    return (
      <Card as="section" variant="accent" size="section">
        <h2 className={styles.cardTitle}>{deloading ? 'Разгрузка' : 'Пора неделю полегче'}</h2>
        <DeloadPlanContent exerciseId={summary.exerciseId} />
      </Card>
    );
  }
  if (summary.status === 'PLATEAU') {
    return (
      <Card as="section" size="section">
        <h2 className={styles.cardTitle}>Плато: что попробовать</h2>
        <p className={styles.plain}>{insights.recommendation.message}</p>
        <PlateauTips />
      </Card>
    );
  }
  if (insights.recommendation.type === 'NOT_ENOUGH_DATA') {
    return null;
  }
  return (
    <Card as="section">
      <p className={styles.plain}>{insights.recommendation.message}</p>
    </Card>
  );
}

function History({ summary, progress }: { summary: ExerciseSummary; progress: ExerciseProgress | undefined }) {
  if (!progress) {
    return <p className={styles.muted}>Загружаем…</p>;
  }
  if (progress.points.length === 0) {
    return <p className={styles.muted}>За этот период тренировок с упражнением не было — выберите период побольше.</p>;
  }
  const rows = historyRows(progress.points, summary.trackingType);
  return (
    <ul className={styles.history}>
      {rows.map((row) => (
        <li key={row.date} className={styles.historyRow}>
          <span className={styles.historyDate}>{formatShortDate(row.date)}</span>
          <span className={styles.historyBest}>
            {row.best}
            <span className={styles.muted}>{row.sets}</span>
          </span>
          {row.strength && <span className={styles.historyStrength}>{row.strength}</span>}
        </li>
      ))}
    </ul>
  );
}
