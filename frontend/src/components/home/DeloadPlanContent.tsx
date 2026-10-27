import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../../api/analytics';
import { formatKg } from '../../lib/format';
import { DeloadAction } from './DeloadAction';
import styles from './Home.module.css';

/**
 * Содержимое плана разгрузочной недели. Один компонент на два места:
 * окно с главной ("План разгрузки") и блок на странице упражнения.
 */
export function DeloadPlanContent({ exerciseId, withAction = true }: { exerciseId: string; withAction?: boolean }) {
  const plan = useQuery({
    queryKey: ['deload-plan', exerciseId],
    queryFn: () => analyticsApi.deloadPlan(exerciseId),
  });

  if (plan.isPending) {
    return <p className={styles.muted}>Составляем план…</p>;
  }
  if (plan.isError) {
    return <p className={styles.muted}>{plan.error.message}</p>;
  }
  return (
    <>
      <p className={styles.signalText}>{plan.data.reason}</p>
      <div className={styles.planGrid}>
        <div className={styles.planCell}>
          <span className={styles.planValue}>{plan.data.durationDays} дн.</span>
          <span className={styles.muted}>длительность</span>
        </div>
        <div className={styles.planCell}>
          <span className={styles.planValue}>
            {plan.data.workingWeightKg != null ? formatKg(plan.data.workingWeightKg) : '—'}
          </span>
          <span className={styles.muted}>рабочий вес*</span>
        </div>
        <div className={styles.planCell}>
          <span className={styles.planValue}>
            {plan.data.sets} × {plan.data.reps}
          </span>
          <span className={styles.muted}>подходы × повторы</span>
        </div>
      </div>
      <ul className={styles.list}>
        {plan.data.notes.map((note) => (
          <li key={note}>{note}</li>
        ))}
      </ul>
      <p className="footnote">
        * {plan.data.intensityPercent}% от вашего расчётного максимума — веса, который вы подняли бы один раз.
      </p>
      {withAction && <DeloadAction plan={plan.data} />}
    </>
  );
}
