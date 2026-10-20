import type { ReactNode } from 'react';
import { Card } from './Card';
import styles from './StatTile.module.css';

interface StatTileProps {
  label: ReactNode;
  value: ReactNode;
  caption?: ReactNode;
  /** подсветить подпись (например, рост) */
  highlightCaption?: boolean;
  variant?: 'default' | 'inverse' | 'accent';
}

/** Плитка "подпись · большая цифра · пояснение". Цвета текста наследуются от варианта карточки. */
export function StatTile({ label, value, caption, highlightCaption = false, variant = 'default' }: StatTileProps) {
  return (
    <Card variant={variant}>
      <span className={styles.label}>{label}</span>
      <span className={styles.value}>{value}</span>
      {caption && <span className={`${styles.caption} ${highlightCaption ? styles.highlight : ''}`}>{caption}</span>}
    </Card>
  );
}
