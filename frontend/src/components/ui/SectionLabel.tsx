import type { ReactNode } from 'react';
import styles from './SectionLabel.module.css';

/**
 * Подпись раздела над карточками: ПРОПИСНЫМИ 12px, серым (дизайн-система: SectionLabel).
 * Разделы страницы подписываются только так; внутри карточки — заголовок карточки (--text-lg).
 * action — одна ссылка справа («Все упражнения →»).
 */
export function SectionLabel({ children, action, id }: { children: ReactNode; action?: ReactNode; id?: string }) {
  return (
    <div className={styles.row}>
      <h2 id={id} className={styles.label}>
        {children}
      </h2>
      {action && <span className={styles.action}>{action}</span>}
    </div>
  );
}
