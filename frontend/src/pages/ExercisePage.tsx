import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  analyticsApi,
  type ExerciseInsights,
  type ExerciseProgress,
  type ExerciseSummary,
  type ProgressPeriod,
} from '../api/analytics';
import { workoutsApi, type CatalogExercise } from '../api/workouts';
import { ExerciseImage } from '../components/ExerciseImage';
import { ExerciseInfo } from '../components/ExerciseInfo';
import { DeloadPlanContent } from '../components/home/DeloadPlanContent';
import { PlateauTips } from '../components/home/PlateauTips';
import { STATUS_BADGES } from '../components/ExerciseRow';
import { ProgressChart } from '../components/ProgressChart';
import { ProgressLegend } from '../components/ProgressLegend';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { Dialog } from '../components/ui/Dialog';
import { ListRow, ListRowGroup } from '../components/ui/ListRow';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { StatTile } from '../components/ui/StatTile';
import { EQUIPMENT_NAMES } from '../lib/catalog';
import { bestDuration, historyRows, strengthCaption, strengthPoints } from '../lib/exercise';
import { ShareChartButton } from '../components/share/ShareChart';
import { BODY_PART_NAMES } from '../lib/plan';
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
 * insights — плато, перегрузка и совет. Описание упражнения из каталога (картинка, мышцы) — для миниатюры у заголовка.
 */
export function ExercisePage() {
  const { exerciseId = '' } = useParams();
  const [period, setPeriod] = useState<ProgressPeriod>('M3');
  const phone = useMediaQuery('(max-width: 640px)');

  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: analyticsApi.dashboard });
  const progress = useQuery({
    queryKey: ['progress', exerciseId, period],
    queryFn: () => analyticsApi.progress(exerciseId, period),
  });
  const insights = useQuery({
    queryKey: ['insights', exerciseId],
    queryFn: () => analyticsApi.insights(exerciseId),
  });
  // каталог меняется редко — не перезапрашиваем при каждом возврате на вкладку
  const exercise = useQuery({
    queryKey: ['exercise', exerciseId],
    queryFn: () => workoutsApi.exercise(exerciseId),
    staleTime: 60 * 60 * 1000,
  });

  const summary = dashboard.data?.exercises.find((e) => e.exerciseId === exerciseId) ?? null;
  const name = summary?.exerciseName ?? progress.data?.exerciseName ?? insights.data?.exerciseName;

  if (dashboard.isPending) {
    return <p className={styles.muted}>Загружаем…</p>;
  }
  if (dashboard.isError) {
    return <p className={styles.muted}>{dashboard.error.message}</p>;
  }
  const back = { to: '/exercises', label: 'Все упражнения' };
  if (!summary) {
    return (
      <div className={styles.page}>
        <PageHeader
          back={back}
          title={name ?? exercise.data?.name ?? 'Упражнение'}
          subtitle={exercise.data && <HowTo exercise={exercise.data} />}
        />
        <p className={styles.muted}>Пока нет завершённых тренировок с этим упражнением.</p>
      </div>
    );
  }

  const badge = summary.status === 'NORMAL' ? null : STATUS_BADGES[summary.status];
  const timed = summary.trackingType === 'TIME';

  return (
    <div className={styles.page}>
      <PageHeader
        back={back}
        title={summary.exerciseName}
        subtitle={
          <div className={styles.subtitleRow}>
            {exercise.data && <HowTo exercise={exercise.data} />}
            {badge && <Badge variant={badge.variant}>{badge.label}</Badge>}
          </div>
        }
      />

      <SegmentedControl legend="Период" options={PERIODS} value={period} onChange={setPeriod} />

      <Tiles summary={summary} progress={progress.data} insights={insights.data} period={period} compact={phone} />

      {/* ПК: график и совет — слева (им нужна ширина), история по тренировкам — справа узким списком.
          Телефон: график → совет → история */}
      <PageColumns
        aside={
          <section className={styles.section}>
            <SectionLabel>История</SectionLabel>
            <History summary={summary} progress={progress.data} />
            <Link to={`/history?exercise=${summary.exerciseId}`} className={styles.historyLink}>
              Все тренировки с этим упражнением →
            </Link>
          </section>
        }
      >
        {/* у упражнений на время нет веса — нет и силы (1ПМ), график силы для них не строим */}
        {!timed && (
          <Card as="section" size="section">
            <div className={styles.cardHead}>
              <h2 className={styles.cardTitle}>Сила*</h2>
              {progress.data && progress.data.points.some((p) => p.e1rmKg != null) && (
                <ShareChartButton
                  exerciseName={summary.exerciseName}
                  periodCaption={PERIOD_CAPTION[period]}
                  strengthKg={summary.strengthKg ?? summary.currentE1rmKg}
                  changePercent={progress.data.changePercent}
                  points={strengthPoints(progress.data.points)}
                  record={summary.status === 'RECORD'}
                />
              )}
            </div>
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

        {!timed && (
          <p className="footnote">
            * Сила — сколько вы подняли бы один раз, посчитано по лучшему подходу тренировки (формула Эпли).
          </p>
        )}
      </PageColumns>
    </div>
  );
}

/**
 * Под заголовком: кнопка-пилюля "Как выполнять" с миниатюрой и рядом "часть тела · снаряд".
 * По нажатию — карточка упражнения: картинки, мышцы, техника.
 */
function HowTo({ exercise }: { exercise: CatalogExercise }) {
  const [open, setOpen] = useState(false);
  return (
    <div className={styles.howToRow}>
      <button type="button" className={styles.howTo} onClick={() => setOpen(true)}>
        <ExerciseImage imageKey={exercise.imageKey} />
        Как выполнять
      </button>
      <span className={styles.muted}>
        {BODY_PART_NAMES[exercise.bodyPart]} · {EQUIPMENT_NAMES[exercise.equipment].toLowerCase()}
      </span>
      <Dialog open={open} onClose={() => setOpen(false)} title={exercise.name}>
        <ExerciseInfo exercise={exercise} />
      </Dialog>
    </div>
  );
}

function Tiles({
  summary,
  progress,
  insights,
  period,
  compact,
}: {
  summary: ExerciseSummary;
  progress: ExerciseProgress | undefined;
  insights: ExerciseInsights | undefined;
  period: ProgressPeriod;
  /** телефон: плитки 2 × 2 компактные — четыре крупные плитки занимали весь первый экран */
  compact: boolean;
}) {
  const last = formatExerciseResult(summary) ?? '—';
  const sessions = insights?.sessionsCount;

  if (summary.trackingType === 'TIME') {
    const best = progress ? bestDuration(progress.points) : null;
    return (
      <div className={styles.tiles}>
        <StatTile
          compact={compact}
          variant="inverse"
          label="Лучшее время"
          value={best != null ? formatClock(best) : '—'}
          caption={PERIOD_CAPTION[period]}
        />
        <StatTile
          compact={compact}
          label="Последний результат"
          value={last}
          caption={formatShortDate(summary.lastPerformedAt)}
        />
        <StatTile compact={compact} label="Тренировок" value={sessions ?? '—'} caption="с этим упражнением" />
      </div>
    );
  }

  const change = progress?.changePercent;
  return (
    <div className={styles.tiles}>
      <StatTile
        compact={compact}
        variant="inverse"
        label="Сила*"
        value={formatKgOrDash(summary.strengthKg ?? summary.currentE1rmKg)}
        caption={
          summary.strengthStale
            ? strengthCaption(summary)
            : change != null
              ? `${formatChange(change)} ${PERIOD_CAPTION[period]}`
              : 'мало данных за период'
        }
        highlightCaption={change != null && change > 0}
      />
      <StatTile
        compact={compact}
        label="Последний подход"
        value={last}
        caption={formatShortDate(summary.lastPerformedAt)}
      />
      <StatTile
        compact={compact}
        label="Объём за неделю"
        value={progress?.weeklyVolumeKg != null ? formatTonnage(progress.weeklyVolumeKg) : '—'}
        caption="вес × повторы"
      />
      <StatTile compact={compact} label="Тренировок" value={sessions ?? '—'} caption="с этим упражнением" />
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
      // обычная карточка, а не оранжевая: внутри много текста и сносок, на оранжевом они не читаются.
      // «Внимание» уже видно по бейджу «перегрузка» под заголовком
      <Card as="section" size="section">
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
  const timed = summary.trackingType === 'TIME';
  // строка: дата · лучший подход и сколько подходов · справа сила (у упражнений на время — лучшее время)
  return (
    <ListRowGroup>
      {rows.map((row) => (
        <ListRow
          key={row.date}
          title={formatShortDate(row.date)}
          text={timed ? row.sets : `${row.best} · ${row.sets}`}
          value={timed ? row.best : row.strength}
        />
      ))}
    </ListRowGroup>
  );
}

/** Сила — лучшее за 4 недели (с сервера); у старых ответов без поля — 1ПМ последней тренировки */
function formatKgOrDash(kg: number | null): string {
  return kg != null ? formatKg(kg) : '—';
}
