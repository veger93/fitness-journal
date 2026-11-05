import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { analyticsApi } from '../api/analytics';
import { workoutsApi, type Workout } from '../api/workouts';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { Select } from '../components/ui/Select';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { formatTonnage } from '../lib/format';
import { groupByMonth, recordsByWorkout } from '../lib/history';
import { formatDistance, formatDuration, summarizeSets } from '../lib/workout';
import styles from './ListPages.module.css';

// «Ср, 4 нояб.»: год и так виден в подписи месяца
const dayFormat = new Intl.DateTimeFormat('ru-RU', { weekday: 'short', day: 'numeric', month: 'short' });

const DESKTOP = '(min-width: 900px)';

/**
 * История как дневник: по карточке на тренировку — дата, комплекс и сразу все упражнения с весами.
 * Чтобы вспомнить "что я делал в четверг", не нужно открывать каждую тренировку.
 * Подгружается по 10 тренировок (useInfiniteQuery), разбита по месяцам.
 *
 * Фильтр по упражнению живёт в адресе (?exercise=<id>): со страницы упражнения можно дать ссылку
 * "все тренировки с ним", а кнопка "назад" в браузере вернёт прежний фильтр.
 */
export function HistoryPage() {
  const [params, setParams] = useSearchParams();
  const exerciseId = params.get('exercise') ?? '';
  const desktop = useMediaQuery(DESKTOP);

  const journal = useInfiniteQuery({
    // фильтр в ключе: у каждого фильтра свой кэш, переключение назад — без нового запроса
    queryKey: ['history', 'journal', exerciseId],
    queryFn: ({ pageParam }) => workoutsApi.journal(pageParam, 10, exerciseId || undefined),
    initialPageParam: 0,
    // следующая страница есть, пока не дошли до последней
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  // список для фильтра — упражнения, которые уже были в тренировках (из той же сводки, что и главная)
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: analyticsApi.dashboard });
  // рекорды — чтобы отметить тренировки, где они поставлены (тот же ключ и запрос, что на странице рекордов — общий кэш)
  const records = useQuery({ queryKey: ['records'], queryFn: () => analyticsApi.records(200) });
  const recordMap = recordsByWorkout(records.data ?? []);

  const workouts = journal.data?.pages.flatMap((page) => page.content) ?? [];
  const exercises = dashboard.data?.exercises ?? [];

  // фильтр: на ПК — справа и не уезжает при прокрутке, на телефоне — сверху над лентой
  const filter = exercises.length > 0 && (
    <Select label="Упражнение" value={exerciseId} onChange={(value) => setParams(value ? { exercise: value } : {})}>
      <option value="">Все тренировки</option>
      {exercises.map((e) => (
        <option key={e.exerciseId} value={e.exerciseId}>
          {e.exerciseName}
        </option>
      ))}
    </Select>
  );

  return (
    <div className={styles.page}>
      <PageHeader title="История" />
      <PageColumns
        stickyAside
        aside={
          desktop &&
          filter && (
            <>
              {filter}
              <p className="footnote">Покажем только тренировки, где было это упражнение — его строка выделена.</p>
            </>
          )
        }
      >
        {!desktop && filter}

        {journal.isPending && <p className={styles.muted}>Загружаем…</p>}
        {journal.isError && <p className={styles.muted}>{journal.error.message}</p>}
        {journal.data && workouts.length === 0 && (
          <EmptyState
            title={exerciseId ? 'С этим упражнением тренировок нет' : 'Тренировок пока нет'}
            text={
              exerciseId
                ? 'Выберите другое упражнение или «Все тренировки».'
                : 'Завершите первую тренировку — здесь появится дневник: что делали, с каким весом, где поставили рекорд.'
            }
            action={exerciseId ? undefined : { to: '/workout', label: 'Начать тренировку' }}
          />
        )}

        {groupByMonth(workouts).map((month) => (
          <section key={month.key} className={styles.section} aria-label={month.title}>
            <SectionLabel>{month.title}</SectionLabel>
            {month.workouts.map((workout) => (
              <JournalDay
                key={workout.id}
                workout={workout}
                recordExercises={recordMap.get(workout.id)}
                highlight={exerciseId}
              />
            ))}
          </section>
        ))}

        {journal.hasNextPage && (
          <Button variant="secondary" onClick={() => journal.fetchNextPage()} disabled={journal.isFetchingNextPage}>
            {journal.isFetchingNextPage ? 'Загружаем…' : 'Показать ещё'}
          </Button>
        )}
      </PageColumns>
    </div>
  );
}

function JournalDay({
  workout,
  recordExercises,
  highlight,
}: {
  workout: Workout;
  /** catalog id упражнений с рекордом в этой тренировке */
  recordExercises: Set<string> | undefined;
  /** выбранное в фильтре упражнение — выделяем его строку */
  highlight: string;
}) {
  const exercises = workout.exercises.filter((e) => e.sets.length > 0);
  const distanceM = exercises
    .filter((e) => e.trackingType === 'DISTANCE')
    .flatMap((e) => e.sets.filter((s) => !s.warmup))
    .reduce((sum, s) => sum + (s.distanceM ?? 0), 0);
  const facts = [
    formatDuration(workout.durationSec ?? 0),
    workout.tonnageKg > 0 && formatTonnage(workout.tonnageKg),
    distanceM > 0 && formatDistance(distanceM),
  ].filter(Boolean);

  return (
    <Card as="article">
      <header className={styles.dayHead}>
        <div className={styles.dayTitleRow}>
          <h3 className={styles.dayTitle}>
            {capitalize(dayFormat.format(new Date(workout.startedAt ?? workout.completedAt ?? '')))}
          </h3>
          {recordExercises && <Badge variant="inverse">рекорд</Badge>}
        </div>
        <span className={styles.muted}>
          {workout.name} · {facts.join(' · ')}
        </span>
      </header>
      <ul className={styles.dayExercises}>
        {exercises.map((exercise) => (
          <li
            key={exercise.id}
            className={`${styles.dayExercise} ${exercise.exerciseId === highlight ? styles.dayExerciseActive : ''}`}
          >
            <span className={styles.dayExerciseName}>
              {exercise.exerciseName}
              {recordExercises?.has(exercise.exerciseId) && (
                <span className={styles.recordMark} title="рекорд">
                  {' '}
                  ★<span className="visually-hidden"> рекорд</span>
                </span>
              )}
            </span>
            <span>{summarizeSets(exercise.trackingType, exercise.sets, exercise.assisted)}</span>
          </li>
        ))}
      </ul>
      <Link to={`/history/${workout.id}`} className={styles.dayLink}>
        Подробнее →
      </Link>
    </Card>
  );
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
