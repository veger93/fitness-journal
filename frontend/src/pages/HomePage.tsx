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
import { completedToday, todayOf } from '../lib/plan';
import { workoutsApi } from '../api/workouts';
import { TodayPlanCard } from '../components/home/TodayPlanCard';
import { MonthSummary } from '../components/report/MonthSummary';
import { currentMonth } from '../lib/month';
import { formatChange, formatKg, formatMonth, formatExerciseResult, formatTonnage } from '../lib/format';
import { useActingClient } from '../trainer/ActingClientProvider';
import { EmailVerifyCard } from '../components/account/EmailVerifyCard';
import styles from './HomePage.module.css';

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
        {/* идёт тренировка — главное действие "вернуться к ней"; сегодня по плану — кнопка "Начать" в карточке плана */}
        {current.data ? (
          <ButtonLink to="/workout/active">Продолжить тренировку</ButtonLink>
        ) : (
          <ButtonLink to="/workout" variant={plannedPending ? 'secondary' : 'primary'}>
            {plannedPending ? 'Другая тренировка' : 'Начать тренировку'}
          </ButtonLink>
        )}
      </div>

      {/* email не подтверждён — напоминание с полем для кода (сама карточка решает, показываться ли) */}
      <EmailVerifyCard />

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

  return (
    <>
      <div className={styles.tiles}>
        <StatTile
          variant="inverse"
          label="Поднято за неделю"
          value={formatTonnage(dashboard.weekTonnageKg)}
          caption={
            dashboard.weekTonnageChangePercent == null
              ? 'на прошлой неделе тренировок не было'
              : `${formatChange(dashboard.weekTonnageChangePercent)} к прошлой`
          }
          highlightCaption={dashboard.weekTonnageChangePercent != null}
        />
        {focus?.currentE1rmKg != null && (
          <StatTile
            label={`Сила: ${focus.exerciseName.toLowerCase()}*`}
            value={formatKg(focus.currentE1rmKg)}
            caption="расчётный максимум"
          />
        )}
        <StatTile
          label="Тренировок"
          value={dashboard.workoutsThisMonth}
          caption={`в ${formatMonthIn(dashboard.month)}`}
        />
        <RiskTile exercises={dashboard.exercises} />
      </div>

      {focus && <FocusChart exercise={focus} />}

      <SignalCards exercises={dashboard.exercises} />
      <ExerciseTicker exercises={dashboard.exercises} />

      <p className="footnote">* Сила — сколько ты поднял бы один раз, посчитано по твоим подходам.</p>
    </>
  );
}

function RiskTile({ exercises }: { exercises: ExerciseSummary[] }) {
  const high = exercises.filter((e) => e.riskLevel === 'HIGH');
  if (high.length > 0) {
    return (
      <StatTile
        variant="accent"
        label="Риск перегрузки"
        value="Высокий"
        caption={high.map((e) => e.exerciseName.toLowerCase()).join(', ')}
      />
    );
  }
  const medium = exercises.some((e) => e.riskLevel === 'MEDIUM');
  return (
    <StatTile
      label="Риск перегрузки"
      value={medium ? 'Средний' : 'Низкий'}
      caption={medium ? 'следите за восстановлением' : 'всё в порядке'}
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
      <div className={styles.chartHead}>
        <h2 className={styles.chartTitle}>
          <Link to={`/exercises/${id}`} className={styles.chartLink}>
            {exercise.exerciseName} →
          </Link>
        </h2>
        {lastSet && <span className={styles.muted}>последний подход: {lastSet}</span>}
      </div>
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
