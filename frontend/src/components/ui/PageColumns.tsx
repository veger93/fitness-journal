import type { ReactNode } from 'react';
import styles from './PageColumns.module.css';

/**
 * Две колонки на ПК (дизайн-система: PageLayout, «2fr | 1fr»): главное слева, сводка и фильтры справа.
 * Так узкая страница занимает всю ширину контента, а не колонку 720px слева.
 * На телефоне и планшете — одна колонка: сначала main, потом aside.
 * stickyAside — правая колонка не уезжает при прокрутке длинного списка (фильтр истории).
 */
export function PageColumns({
  children,
  aside,
  stickyAside = false,
}: {
  children: ReactNode;
  aside?: ReactNode;
  stickyAside?: boolean;
}) {
  return (
    <div className={styles.columns}>
      <div className={styles.main}>{children}</div>
      {aside && <aside className={`${styles.aside} ${stickyAside ? styles.sticky : ''}`}>{aside}</aside>}
    </div>
  );
}
