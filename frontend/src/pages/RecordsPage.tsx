import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useNewRecord } from '../hooks/useNewRecord';
import { analyticsApi, type PersonalRecord, type RunningSummary } from '../api/analytics';
import { formatClock, formatDistance } from '../lib/workout';
import { ListRow, ListRowGroup } from '../components/ui/ListRow';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { EmptyState } from '../components/ui/EmptyState';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { formatKg, formatSet, formatShortDate } from '../lib/format';
import styles from './ListPages.module.css';

type View = 'best' | 'history';

const VIEWS: { value: View; label: string }[] = [
  { value: 'best', label: 'Лучшие' },
  { value: 'history', label: 'История' },
];

/**
 * Рекорды двумя способами (строками ListRow, как все перечни):
 * "Лучшие" — текущий личный рекорд в каждом упражнении (то, чем хочется похвастаться);
 * "История" — все рекорды по месяцам.
 */
export function RecordsPage() {
  const [view, setView] = useState<View>('best');
  const records = useQuery({ queryKey: ['records'], queryFn: () => analyticsApi.records(200) });
  const running = useQuery({ queryKey: ['running'], queryFn: analyticsApi.running });
  const data = records.data ?? [];
  const hasRunning = running.data != null && !running.data.empty;
  // открыли рекорды — точка на вкладке гаснет (до следующего рекорда)
  const { fresh, markSeen } = useNewRecord();
  useEffect(() => {
    if (fresh && records.isSuccess) {
      markSeen();
    }
  }, [fresh, records.isSuccess, markSeen]);

  const empty = records.data?.length === 0 && running.data?.empty !== false;

  return (
    <div className={styles.page}>
      <PageHeader title="Рекорды" />
      {records.isPending && <p className={styles.muted}>Загружаем…</p>}
      {records.isError && <p className={styles.muted}>{records.error.message}</p>}
      {empty && (
        <EmptyState
          title="Рекордов пока нет"
          text="Первый появится, когда вы превзойдёте свой прошлый результат в любом упражнении."
          action={{ to: '/workout', label: 'Начать тренировку' }}
        />
      )}

      {!empty && records.isSuccess && (
        <PageColumns
          aside={
            <>
              {hasRunning && <RunningRecords running={running.data!} />}
              <p className="footnote">
                ★ — новый максимальный вес. ↗ — стали сильнее: больше повторов или вес при тех же повторах. Сила —
                сколько вы подняли бы один раз, посчитано по подходу.
              </p>
            </>
          }
        >
          <SegmentedControl legend="Какие рекорды показать" options={VIEWS} value={view} onChange={setView} />

          {view === 'best' && data.length > 0 && (
            <section className={styles.section}>
              <SectionLabel>Лучшее в каждом упражнении</SectionLabel>
              <ListRowGroup>
                {bestPerExercise(data).map((record) => (
                  <RecordRow key={record.exerciseId} record={record} />
                ))}
              </ListRowGroup>
            </section>
          )}

          {view === 'history' &&
            groupByMonth(data).map(({ month, items }) => (
              <section key={month} className={styles.section}>
                <SectionLabel>{month}</SectionLabel>
                <ListRowGroup>
                  {items.map((record) => (
                    <RecordRow key={`${record.exerciseId}-${record.achievedAt}`} record={record} />
                  ))}
                </ListRowGroup>
              </section>
            ))}
        </PageColumns>
      )}
    </div>
  );
}

/**
 * Рекорды приходят новыми сверху, а рекорд может только расти,
 * значит самый свежий рекорд упражнения и есть его текущий лучший результат.
 */
export function bestPerExercise(records: PersonalRecord[]): PersonalRecord[] {
  const seen = new Set<string>();
  return records.filter((record) => !seen.has(record.exerciseId) && seen.add(record.exerciseId));
}

/** [{ month: "октябрь 2026", items }] в порядке от новых к старым */
export function groupByMonth(records: PersonalRecord[]): { month: string; items: PersonalRecord[] }[] {
  const groups = new Map<string, PersonalRecord[]>();
  for (const record of records) {
    const date = new Date(record.achievedAt);
    const month = `${monthName.format(date)} ${date.getFullYear()}`; // без "г.", который добавляет Intl
    groups.set(month, [...(groups.get(month) ?? []), record]);
  }
  return [...groups].map(([month, items]) => ({ month, items }));
}

const monthName = new Intl.DateTimeFormat('ru-RU', { month: 'long' });

/**
 * Строка рекорда: ★ — новый максимальный вес («было 90 кг»), ↗ — только сила (больше повторов при том же весе и т. п.).
 * Одна строка вместо карточки с бейджами и двумя сносками: то же самое, но список читается сразу.
 */
export function recordSummary(record: PersonalRecord): { mark: string; tone: 'inverse' | 'plain'; text: string } {
  const weight = record.records.find((item) => item.type === 'WEIGHT');
  const strength = record.records.find((item) => item.type === 'E1RM');
  const parts = [formatShortDate(record.achievedAt)];
  if (weight) {
    if (weight.deltaKg != null) {
      parts.push(`было ${formatKg(weight.valueKg - weight.deltaKg)}`);
    }
    // рост силы здесь не пишем: при новом весе он почти всегда есть, а строка на телефоне вырастает до 3 строк
    return { mark: '★', tone: 'inverse', text: parts.join(' · ') };
  }
  if (strength) {
    parts.push(
      strength.hint ?? (strength.deltaKg != null ? `сила +${formatKg(strength.deltaKg)}` : formatKg(strength.valueKg)),
    );
  }
  return { mark: '↗', tone: 'plain', text: parts.join(' · ') };
}

function RecordRow({ record }: { record: PersonalRecord }) {
  const { mark, tone, text } = recordSummary(record);
  return (
    <ListRow
      to={`/exercises/${record.exerciseId}`}
      mark={mark}
      tone={tone}
      title={record.exerciseName}
      text={text}
      value={formatSet(record.weightKg, record.reps) ?? '—'}
    />
  );
}

/** Рекорды бега — отдельным списком: у них другие величины (км и темп), а не вес. */
function RunningRecords({ running }: { running: RunningSummary }) {
  return (
    <section className={styles.section}>
      <SectionLabel action={<Link to="/running">Подробнее →</Link>}>Бег</SectionLabel>
      <ListRowGroup>
        {running.longestRun?.distanceM != null && (
          <ListRow title="Самая длинная" value={formatDistance(running.longestRun.distanceM)} />
        )}
        {running.bestPace?.paceSecPerKm != null && (
          <ListRow title="Лучший темп" value={`${formatClock(running.bestPace.paceSecPerKm)} /км`} />
        )}
      </ListRowGroup>
    </section>
  );
}
