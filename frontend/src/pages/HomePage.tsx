import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { analyticsApi, type Dashboard, type ExerciseSummary } from '../api/analytics';
import { authApi } from '../api/auth';
import { ExerciseTicker } from '../components/home/ExerciseTicker';
import { SignalCards } from '../components/home/SignalCards';
import { ProgressChart } from '../components/ProgressChart';
import { ProgressLegend } from '../components/ProgressLegend';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { StatTile } from '../components/ui/StatTile';
import { useCurrentWorkout } from '../hooks/useWorkout';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { completedToday, todayOf } from '../lib/plan';
import { strengthCaption, strengthPoints } from '../lib/exercise';
import { ShareChartButton } from '../components/share/ShareChart';
import { workoutsApi } from '../api/workouts';
import { TodayPlanCard } from '../components/home/TodayPlanCard';
import { MonthSummary } from '../components/report/MonthSummary';
import { currentMonth } from '../lib/month';
import { formatChange, formatKg, formatMonth, formatExerciseResult, formatTonnage } from '../lib/format';
import { useActingClient } from '../trainer/ActingClientProvider';
import styles from './HomePage.module.css';

/** Телефон: та же граница, что у раскладки в CSS (Home.module.css, max-width: 640px) */
const PHONE = '(max-width: 640px)';

/**
 * Главная. Данные — три запроса через TanStack Query:
 * сводка (dashboard), график и подсказка по главному упражнению (progress, insights).
 * Query сам кэширует ответы, повторяет упавшие запросы и даёт isPending / isError — без ручных useEffect.
 */
export function HomePage() {
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const { client: acting } = useActingClient();
  const dashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: analyticsApi.dashboard,
  });
  const current = useCurrentWorkout();
  const plan = useQuery({ queryKey: ['plan'], queryFn: workoutsApi.plan });
  // последние тренировки — понять, сделана ли сегодняшняя по плану (ключ под 'history': обновится после "Завершить")
  const recent = useQuery({ queryKey: ['history', 'recent'], queryFn: () => workoutsApi.journal(0, 5) });
  const todayPlanned = plan.data?.days.find((d) => d.dayOfWeek === todayOf())?.template ?? null;
  const plannedPending = todayPlanned !== null && !completedToday(recent.data?.content ?? [], todayPlanned.id);
  const phone = useMediaQuery(PHONE);

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <p className={styles.greeting}>
            {/* тренер в дневнике клиента — подписываем, чей он */}
            {acting ? `Дневник: ${acting.displayName}` : `Привет, ${me.data?.displayName || me.data?.email || '…'}`}
          </p>
          <h1 className={styles.title}>
            {dashboard.data ? `Прогресс за ${formatMonth(dashboard.data.month)}` : 'Прогресс'}
          </h1>
          <Link to={`/reports/${currentMonth()}`} className={styles.reportLink}>
            Отчёт за месяц →
          </Link>
        </div>
        {/* идёт тренировка — главное действие "вернуться к ней"; сегодня по плану — кнопка "Начать" в карточке плана.
            На телефоне "Другая тренировка" не дублируем: она есть во вкладке "Тренировка" внизу, а место наверху дорогое */}
        {current.data ? (
          <ButtonLink to="/workout/active">Продолжить тренировку</ButtonLink>
        ) : phone && plannedPending ? null : (
          <ButtonLink to="/workout" variant={plannedPending ? 'secondary' : 'primary'}>
            {plannedPending ? 'Другая тренировка' : 'Начать тренировку'}
          </ButtonLink>
        )}
      </div>

      {/* пока идёт тренировка, "сегодня по плану" не нужен — она уже начата */}
      {plan.data && !current.data && <TodayPlanCard plan={plan.data} recent={recent.data?.content ?? []} />}

      <MonthSummary />

      {dashboard.isPending && <p className={styles.muted}>Загружаем…</p>}

      {dashboard.isError && (
        <Card>
          <p className={styles.plain}>{dashboard.error.message}</p>
          <Button variant="secondary" className={styles.retry} onClick={() => dashboard.refetch()}>
            Повторить
          </Button>
        </Card>
      )}

      {dashboard.data?.empty && <EmptyState />}
      {dashboard.data && !dashboard.data.empty && <Content dashboard={dashboard.data} />}
    </div>
  );
}

function EmptyState() {
  return (
    <Card variant="inverse" size="section">
      <h2 className={styles.cardTitle}>Начните первую тренировку</h2>
      <p className={styles.onInverse}>
        Записывайте подходы — здесь появятся прогресс, рекорды и подсказки, когда пора добавить вес, а когда отдохнуть.
      </p>
    </Card>
  );
}

function Content({ dashboard }: { dashboard: Dashboard }) {
  const focus = dashboard.exercises.find((e) => e.exerciseId === dashboard.focusExerciseId) ?? null;
  // на телефоне всё идёт столбиком, поэтому порядок другой: сначала "что сделать", потом цифры —
  // и сигналы строками (иначе главная занимает 4–5 экранов)
  const phone = useMediaQuery(PHONE);

  // "Неделя" — только общие цифры по всем упражнениям. Всё про одно упражнение (сила, последний подход)
  // живёт в его карточке с графиком: раньше "Сила: жим" стояла среди общих плиток, и было непонятно, что к чему
  const week = (
    <section className={styles.week} aria-labelledby="week-title">
      <h2 id="week-title" className={styles.weekTitle}>
        Неделя
      </h2>
      <div className={`${styles.tiles} ${styles.tiles3}`}>
        <StatTile
          compact={phone}
          variant="inverse"
          label={phone ? 'Поднято' : 'Поднято за неделю'}
          value={formatTonnage(dashboard.weekTonnageKg)}
          caption={
            dashboard.weekTonnageChangePercent == null
              ? 'прошлая неделя без тренировок'
              : `${formatChange(dashboard.weekTonnageChangePercent)} к прошлой`
          }
          highlightCaption={dashboard.weekTonnageChangePercent != null}
        />
        <StatTile
          compact={phone}
          label="Тренировок"
          value={dashboard.workoutsThisMonth}
          caption={`в ${formatMonthIn(dashboard.month)}`}
        />
        <RiskTile exercises={dashboard.exercises} compact={phone} />
      </div>
    </section>
  );

  return (
    <>
      {phone && <SignalCards exercises={dashboard.exercises} compact />}
      {week}
      {focus && <FocusChart exercise={focus} />}
      {!phone && <SignalCards exercises={dashboard.exercises} />}
      <ExerciseTicker exercises={dashboard.exercises} />

      <p className="footnote">* Сила — сколько ты поднял бы один раз, посчитано по твоим подходам.</p>
    </>
  );
}

function RiskTile({ exercises, compact }: { exercises: ExerciseSummary[]; compact: boolean }) {
  const label = compact ? 'Риск' : 'Риск перегрузки';
  const high = exercises.filter((e) => e.riskLevel === 'HIGH');
  if (high.length > 0) {
    return (
      <StatTile
        compact={compact}
        variant="accent"
        label={label}
        value="Высокий"
        caption={high.map((e) => e.exerciseName.toLowerCase()).join(', ')}
      />
    );
  }
  const medium = exercises.some((e) => e.riskLevel === 'MEDIUM');
  return (
    <StatTile
      compact={compact}
      label={label}
      value={medium ? 'Средний' : 'Низкий'}
      caption={medium ? 'следите за отдыхом' : 'всё в порядке'}
    />
  );
}

function FocusChart({ exercise }: { exercise: ExerciseSummary }) {
  const id = exercise.exerciseId;
  // queryKey включает id: другое упражнение — другой кэш
  const progress = useQuery({
    queryKey: ['progress', id, 'M3'],
    queryFn: () => analyticsApi.progress(id, 'M3'),
  });
  const insights = useQuery({
    queryKey: ['insights', id],
    queryFn: () => analyticsApi.insights(id),
  });
  const lastSet = formatExerciseResult(exercise);
  const plateau = insights.data?.plateau;

  return (
    <Card as="section" size="section">
      {/* длинное название переносится, значок "поделиться" остаётся справа сверху (не уезжает на новую строку) */}
      <div className={styles.chartHead}>
        <h2 className={styles.chartTitle}>
          <Link to={`/exercises/${id}`} className={styles.chartLink}>
            {exercise.exerciseName} →
          </Link>
        </h2>
        {progress.data && progress.data.points.some((p) => p.e1rmKg != null) && (
          <ShareChartButton
            exerciseName={exercise.exerciseName}
            periodCaption="за 3 месяца"
            strengthKg={exercise.strengthKg ?? exercise.currentE1rmKg}
            changePercent={progress.data.changePercent}
            points={strengthPoints(progress.data.points)}
            record={exercise.status === 'RECORD'}
          />
        )}
      </div>
      <Strength exercise={exercise} lastSet={lastSet} />
      <ProgressLegend plateauDays={plateau?.detected ? plateau.daysSinceProgress : null} />
      {progress.data ? (
        <ProgressChart points={progress.data.points} plateauFrom={plateau?.detected ? plateau.lastProgressAt : null} />
      ) : (
        <div className={`${styles.muted} ${styles.chartPlaceholder}`}>
          {progress.isError ? progress.error.message : 'Загружаем график…'}
        </div>
      )}
      {insights.data && insights.data.recommendation.type !== 'NOT_ENOUGH_DATA' && (
        <p className={styles.advice}>
          <svg
            className={styles.adviceIcon}
            aria-hidden="true"
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M9 18h6" />
            <path d="M10 22h4" />
            <path d="M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2z" />
          </svg>
          <span>{insights.data.recommendation.message}</span>
        </p>
      )}
    </Card>
  );
}

/** "2026-10" -> "октябре" (предложный падеж для "в октябре") */
function formatMonthIn(month: string): string {
  const name = formatMonth(month);
  return name.endsWith('ь') || name.endsWith('й') ? `${name.slice(0, -1)}е` : `${name}е`;
}

/**
 * Сила упражнения и последний подход — рядом, чтобы было видно разницу:
 * "Сила 36,7 кг (лучшее за 4 недели)" и "последний подход 15 кг × 10 — сила в нём 20 кг".
 */
function Strength({ exercise, lastSet }: { exercise: ExerciseSummary; lastSet: string | null }) {
  const strength = exercise.strengthKg ?? exercise.currentE1rmKg;
  const lastStrength = exercise.currentE1rmKg;
  return (
    <div className={styles.strength}>
      {strength != null && (
        <div>
          <span className={styles.muted}>Сила*</span>
          <p className={styles.strengthValue}>{formatKg(strength)}</p>
          <span className={styles.muted}>{strengthCaption(exercise)}</span>
        </div>
      )}
      {lastSet && (
        <div className={styles.lastSet}>
          <span className={styles.muted}>Последний подход</span>
          <p className={styles.lastSetValue}>{lastSet}</p>
          {lastStrength != null && strength != null && lastStrength < strength && (
            <span className={styles.muted}>сила в нём {formatKg(lastStrength)}</span>
          )}
        </div>
      )}
    </div>
  );
}
