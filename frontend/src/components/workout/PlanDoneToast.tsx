import { useEffect, useState } from 'react';
import styles from './PlanDoneToast.module.css';

/** Сколько плашка видна, мс: прочитать «План дня выполнен» хватает, а мешать итогам не успевает */
const VISIBLE_MS = 2600;

/**
 * «План дня выполнен» (макет S4): зелёная плашка сверху поверх итогов тренировки, сама исчезает.
 * role="status" — экранный диктор зачитает её, не перебивая.
 */
export function PlanDoneToast({ done, total }: { done: number; total: number }) {
  const [visible, setVisible] = useState(true);
  useEffect(() => {
    const timer = window.setTimeout(() => setVisible(false), VISIBLE_MS);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <div className={`${styles.toast} ${visible ? '' : styles.hidden}`} role="status">
      <span className={styles.icon} aria-hidden="true">
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M5 12l5 5L20 7" />
        </svg>
      </span>
      <span className={styles.text}>
        <b>План дня выполнен</b>
        <span>
          {done} из {total} подходов
        </span>
      </span>
    </div>
  );
}
