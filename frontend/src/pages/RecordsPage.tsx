import { useQuery } from '@tanstack/react-query';
import { analyticsApi, type PersonalRecord } from '../api/analytics';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { formatKg, formatSet, formatShortDate } from '../lib/format';
import styles from './ListPages.module.css';

const TYPE_LABEL = { WEIGHT: 'Вес', E1RM: 'Сила' } as const;

/** Лента личных рекордов: одна карточка = упражнение в одной тренировке, виды рекорда — бейджами, пояснения — сносками. */
export function RecordsPage() {
  const records = useQuery({ queryKey: ['records'], queryFn: () => analyticsApi.records() });

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Рекорды</h1>
      {records.isPending && <p className={styles.muted}>Загружаем…</p>}
      {records.isError && <p className={styles.muted}>{records.error.message}</p>}
      {records.data?.length === 0 && (
        <p className={styles.muted}>Рекордов пока нет. Первый появится, когда вы превзойдёте свой прошлый результат.</p>
      )}
      {records.data && records.data.length > 0 && (
        <div className={styles.records}>
          {records.data.map((record) => (
            <RecordCard key={`${record.exerciseId}-${record.achievedAt}`} record={record} />
          ))}
        </div>
      )}
      <p className="footnote">
        Вес — самый большой поднятый вес. Сила — сколько вы подняли бы один раз, посчитано по подходу.
      </p>
    </div>
  );
}

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
