import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { analyticsApi, type PersonalRecord } from '../api/analytics';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { formatKg, formatSet, formatShortDate } from '../lib/format';
import styles from './ListPages.module.css';

const TYPE_LABEL = { WEIGHT: 'Вес', E1RM: 'Сила' } as const;

type View = 'best' | 'history';

const VIEWS: { value: View; label: string }[] = [
  { value: 'best', label: 'Лучшие' },
  { value: 'history', label: 'История' },
];

/**
 * Рекорды двумя способами:
 * "Лучшие" — текущий личный рекорд в каждом упражнении (то, чем хочется похвастаться);
 * "История" — все рекорды по месяцам.
 */
export function RecordsPage() {
  const [view, setView] = useState<View>('best');
  const records = useQuery({ queryKey: ['records'], queryFn: () => analyticsApi.records(200) });
  const data = records.data ?? [];

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Рекорды</h1>
      <SegmentedControl legend="Какие рекорды показать" options={VIEWS} value={view} onChange={setView} />
      {records.isPending && <p className={styles.muted}>Загружаем…</p>}
      {records.isError && <p className={styles.muted}>{records.error.message}</p>}
      {records.data?.length === 0 && (
        <p className={styles.muted}>Рекордов пока нет. Первый появится, когда вы превзойдёте свой прошлый результат.</p>
      )}

      {view === 'best' && data.length > 0 && (
        <div className={styles.records}>
          {bestPerExercise(data).map((record) => (
            <RecordCard key={record.exerciseId} record={record} />
          ))}
        </div>
      )}

      {view === 'history' &&
        groupByMonth(data).map(({ month, items }) => (
          <section key={month} className={styles.page}>
            <h2 className={styles.monthTitle}>{month}</h2>
            <div className={styles.records}>
              {items.map((record) => (
                <RecordCard key={`${record.exerciseId}-${record.achievedAt}`} record={record} />
              ))}
            </div>
          </section>
        ))}

      <p className="footnote">
        Вес — самый большой поднятый вес. Сила — сколько вы подняли бы один раз, посчитано по подходу.
      </p>
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

function RecordCard({ record }: { record: PersonalRecord }) {
  const hints = record.records.filter((item) => item.hint);
  return (
    <Card as="article">
      <div className={styles.recordHead}>
        <h2 className={styles.recordName}>{record.exerciseName}</h2>
        <span className={styles.muted}>{formatShortDate(record.achievedAt)}</span>
      </div>
      <span className={styles.recordSet}>{formatSet(record.weightKg, record.reps) ?? '—'}</span>
      <div className={styles.badges}>
        {record.records.map((item) => (
          <Badge key={item.type} variant={item.type === 'WEIGHT' ? 'inverse' : 'neutral'}>
            {TYPE_LABEL[item.type]} {item.deltaKg != null ? `+${formatKg(item.deltaKg)}` : formatKg(item.valueKg)}
          </Badge>
        ))}
      </div>
      {hints.map((item) => (
        <p key={item.type} className="footnote">
          {TYPE_LABEL[item.type]}: {item.hint}
        </p>
      ))}
    </Card>
  );
}
