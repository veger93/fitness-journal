import { formatChange } from '../../lib/format';
import styles from './Report.module.css';

/** Изменение в процентах: рост — акцентом, падение — приглушённо, нечего сравнивать — прочерк. */
export function ChangeText({ percent }: { percent: number | null }) {
  if (percent == null) {
    return <span className={styles.muted}>—</span>;
  }
  return <span className={percent > 0 ? styles.up : styles.muted}>{formatChange(percent)}</span>;
}
