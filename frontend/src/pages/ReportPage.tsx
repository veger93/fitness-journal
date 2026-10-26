import { useQuery } from '@tanstack/react-query';
import { Link, Navigate, useParams } from 'react-router-dom';
import { analyticsApi, type MonthlyReport } from '../api/analytics';
import { ChangeText } from '../components/report/ChangeText';
import { BackLink } from '../components/ui/BackLink';
import { Card } from '../components/ui/Card';
import { StatTile } from '../components/ui/StatTile';
import { formatChange, formatKg, formatTonnage } from '../lib/format';
import { addMonths, currentMonth, monthTitle } from '../lib/month';
import styles from '../components/report/Report.module.css';

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

/**
 * Полный отчёт за месяц: было / стало по силе, тоннажу и тренировкам, рекорды и таблица по упражнениям.
 * Месяц — в адресе (/reports/2026-10): ссылку из письма "отчёт готов" или из истории браузера можно открыть напрямую.
 */
export function ReportPage() {
  const { month = '' } = useParams();
  const valid = MONTH_PATTERN.test(month);
  const report = useQuery({
    queryKey: ['report', month],
    queryFn: () => analyticsApi.monthly(month),
    enabled: valid,
  });

  if (!valid) {
    return <Navigate to={`/reports/${currentMonth()}`} replace />;
  }

  const isCurrent = month >= currentMonth(); // строки "ГГГГ-ММ" сравниваются как даты

  return (
    <div className={styles.page}>
      <BackLink to="/">Главная</BackLink>
      <div>
        <p className={styles.muted}>Отчёт за месяц</p>
        <h1 className={styles.title}>{capitalize(monthTitle(month))}</h1>
      </div>
      <nav className={styles.monthNav} aria-label="Другой месяц">
        <Link to={`/reports/${addMonths(month, -1)}`}>← {monthTitle(addMonths(month, -1))}</Link>
        {!isCurrent && <Link to={`/reports/${addMonths(month, 1)}`}>{monthTitle(addMonths(month, 1))} →</Link>}
      </nav>

      {report.isPending && <p className={styles.muted}>Загружаем…</p>}
      {report.isError && <p className={styles.muted}>{report.error.message}</p>}
      {report.data?.empty && (
        <Card variant="inverse" size="section">
          <h2 className={styles.cardTitle}>Тренировок не было</h2>
          <p className={styles.onInverse}>
            {isCurrent
              ? 'Месяц только идёт — запишите тренировку, и здесь появятся итоги.'
              : 'В этом месяце нет записанных тренировок.'}
          </p>
        </Card>
      )}
      {report.data && !report.data.empty && <ReportContent report={report.data} isCurrent={isCurrent} />}
    </div>
  );
}

function ReportContent({ report, isCurrent }: { report: MonthlyReport; isCurrent: boolean }) {
  return (
    <>
      {isCurrent && <p className={styles.muted}>Месяц ещё идёт — цифры обновятся после каждой тренировки.</p>}
      <div className={styles.tiles}>
        {report.strength?.after != null && (
          <StatTile
            variant="inverse"
            label="Сила в среднем*"
            value={formatKg(report.strength.after)}
            caption={changeCaption(report.strength.changePercent)}
            highlightCaption={(report.strength.changePercent ?? 0) > 0}
          />
        )}
        <StatTile
          label="Поднято за месяц**"
          value={report.tonnage.after != null ? formatTonnage(report.tonnage.after) : '—'}
          caption={changeCaption(report.tonnage.changePercent)}
        />
        <StatTile label="Тренировок" value={report.workouts.count} caption={report.workouts.hint} />
        <StatTile label="Рекордов" value={report.recordsCount} caption="новых максимумов" />
      </div>

      {report.exercises.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.cardTitle}>По упражнениям</h2>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">Упражнение</th>
                <th scope="col">Было*</th>
                <th scope="col">Стало*</th>
                <th scope="col">Изменение</th>
                <th scope="col">Тренировок</th>
              </tr>
            </thead>
            <tbody>
              {report.exercises.map((row) => (
                <tr key={row.exerciseId}>
                  <th scope="row">
                    <Link to={`/exercises/${row.exerciseId}`}>{row.exerciseName}</Link>
                  </th>
                  {/* data-label — подпись ячейки на телефоне, где таблица превращается в карточки */}
                  <td data-label="Было">{row.beforeE1rmKg != null ? formatKg(row.beforeE1rmKg) : '—'}</td>
                  <td data-label="Стало">{row.afterE1rmKg != null ? formatKg(row.afterE1rmKg) : '—'}</td>
                  <td data-label="Изм.">
                    <ChangeText percent={row.changePercent} />
                  </td>
                  <td data-label="Трен.">{row.sessions}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <div>
        {report.strength && <p className="footnote">* Сила — {report.strength.hint}.</p>}
        <p className="footnote">** {capitalize(report.tonnage.hint)}.</p>
      </div>
    </>
  );
}

function changeCaption(percent: number | null): string {
  return percent == null ? 'в прошлом месяце сравнить не с чем' : `${formatChange(percent)} к прошлому`;
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
