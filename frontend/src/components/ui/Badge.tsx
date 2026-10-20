import type { ReactNode } from 'react';
import styles from './Badge.module.css';

/** Бейдж статуса: neutral — информация (плато), inverse — хорошее (рекорд), accent — внимание (перегрузка). */
export function Badge({ variant = 'neutral', children }: { variant?: 'neutral' | 'inverse' | 'accent'; children: ReactNode }) {
  return <span className={`${styles.badge} ${styles[variant]}`}>{children}</span>;
}
