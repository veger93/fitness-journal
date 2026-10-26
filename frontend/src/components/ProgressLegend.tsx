import styles from './ProgressLegend.module.css';

/** Легенда к графику силы: "ты", "норма", зона плато. Над графиком — на главной и на странице упражнения. */
export function ProgressLegend({ plateauDays }: { plateauDays?: number | null }) {
  return (
    <div className={styles.legend}>
      <span className={styles.item}>
        <span className={styles.lineYou} aria-hidden="true" />
        ты
      </span>
      <span className={styles.item}>
        <span className={styles.lineReference} aria-hidden="true" />
        норма для твоего уровня
      </span>
      {plateauDays != null && (
        <span className={styles.item}>
          <span className={styles.zonePlateau} aria-hidden="true" />
          плато {plateauDays} дн.
        </span>
      )}
    </div>
  );
}
