import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { analyticsApi, type MonthlyReport } from '../../api/analytics';
import { formatKg, formatMonth, formatTonnage } from '../../lib/format';
import { summaryMonthToShow } from '../../lib/month';
import { Button, ButtonLink } from '../ui/Button';
import { Card } from '../ui/Card';
import { Dialog } from '../ui/Dialog';
import { ChangeText } from './ChangeText';
import styles from './Report.module.css';

const DISMISSED_KEY = 'vegas.monthSummaryDismissed';

/**
 * localStorage — удобство одного браузера: "эту плашку я уже закрыл".
 * Может быть недоступен (приватный режим) — тогда просто покажем плашку ещё раз, ничего не сломается.
 */
function readDismissed(): string | null {
  try {
    return localStorage.getItem(DISMISSED_KEY);
  } catch {
    return null;
  }
}

function writeDismissed(month: string) {
  try {
    localStorage.setItem(DISMISSED_KEY, month);
  } catch {
    // не сохранилось — не страшно
  }
}

/**
 * Первую неделю нового месяца на главной — плашка "Итоги октября готовы".
 * По кнопке — окно "было / стало" (карта экранов: "Итоги месяца", вариант А), из него — полный отчёт.
 */
export function MonthSummary() {
  const month = summaryMonthToShow();
  const [dismissed, setDismissed] = useState(() => readDismissed());
  const [open, setOpen] = useState(false);
  const report = useQuery({
    queryKey: ['report', month],
    queryFn: () => analyticsApi.monthly(month!),
    // запрос только когда плашка вообще может появиться
    enabled: month !== null && dismissed !== month,
  });

  if (!month || dismissed === month || !report.data || report.data.empty) {
    return null;
  }

  const hide = () => {
    writeDismissed(month);
    setDismissed(month);
  };

  return (
    <>
      <Card as="section" variant="inverse" className={styles.banner}>
        <div>
          <h2 className={styles.cardTitle}>Итоги за {formatMonth(month)} готовы</h2>
          <p className={styles.onInverse}>Сравним с прошлым месяцем: сила, объём, тренировки.</p>
        </div>
        <div className={styles.bannerActions}>
          <Button variant="surface" onClick={() => setOpen(true)}>
            Посмотреть
          </Button>
          <Button variant="secondary" onClick={hide}>
            Скрыть
          </Button>
        </div>
      </Card>
      {open && <SummaryDialog report={report.data} onClose={() => setOpen(false)} />}
    </>
  );
}

function SummaryDialog({ report, onClose }: { report: MonthlyReport; onClose: () => void }) {
  return (
    <Dialog open onClose={onClose} title={`Итоги: ${formatMonth(report.month)}`} subtitle="Сравнение с прошлым месяцем">
      <div className={styles.compare}>
        {report.strength && report.strength.after != null && (
          <Compare
            label="Сила в среднем"
            before={report.strength.before != null ? formatKg(report.strength.before) : null}
            after={formatKg(report.strength.after)}
            percent={report.strength.changePercent}
          />
        )}
        <Compare
          label="Поднято"
          before={report.tonnage.before != null ? formatTonnage(report.tonnage.before) : null}
          after={report.tonnage.after != null ? formatTonnage(report.tonnage.after) : '—'}
          percent={report.tonnage.changePercent}
        />
        <Compare
          label="Тренировок"
          before={String(report.workouts.previousCount)}
          after={String(report.workouts.count)}
          percent={report.workouts.changePercent}
        />
      </div>
      {report.recordsCount > 0 && <p className={styles.muted}>Новых рекордов: {report.recordsCount}.</p>}
      <ButtonLink to={`/reports/${report.month}`} onClick={onClose}>
        Открыть полный отчёт
      </ButtonLink>
    </Dialog>
  );
}

function Compare({
  label,
  before,
  after,
  percent,
}: {
  label: string;
  before: string | null;
  after: string;
  percent: number | null;
}) {
  return (
    <div className={styles.compareCell}>
      <span className={styles.muted}>{label}</span>
      <span className={styles.compareValue}>{after}</span>
      <span className={styles.muted}>
        {before != null ? `было ${before} · ` : ''}
        <ChangeText percent={percent} />
      </span>
    </div>
  );
}
