import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import styles from './ListRow.module.css';

export type ListRowTone = 'accent' | 'plain' | 'inverse';

interface ListRowProps {
  title: ReactNode;
  /** вторая строка: коротко, серым */
  text?: ReactNode;
  /** значок 36px слева: «!», «≈», «★» */
  mark?: string;
  tone?: ListRowTone;
  /** значение справа («120 кг × 5») */
  value?: ReactNode;
  /** строка — ссылка (to) или кнопка (onClick); без них — просто строка */
  to?: string;
  onClick?: () => void;
  disabled?: boolean;
  /** выбранная строка (текущее упражнение тренировки) */
  selected?: boolean;
}

/**
 * Строка списка в карточке (дизайн-система: ListRow): значок · заголовок + вторая строка · значение · ›.
 * Один образец для всех перечней: сигналы, упражнения, рекорды, уведомления, комплексы.
 * Вся строка — одна цель для пальца (≥ 44px). Карточка вокруг — .list (ListRowGroup).
 */
export function ListRow({ title, text, mark, tone = 'plain', value, to, onClick, disabled, selected }: ListRowProps) {
  const interactive = Boolean(to || onClick);
  const body = (
    <>
      {mark && (
        <span className={`${styles.mark} ${styles[tone]}`} aria-hidden="true">
          {mark}
        </span>
      )}
      <span className={styles.body}>
        <span className={styles.title}>{title}</span>
        {text && <span className={styles.text}>{text}</span>}
      </span>
      {value != null && <span className={styles.value}>{value}</span>}
      {interactive && (
        <span className={styles.arrow} aria-hidden="true">
          ›
        </span>
      )}
    </>
  );
  const className = `${styles.row} ${selected ? styles.selected : ''}`;
  if (to) {
    return (
      <Link to={to} className={className}>
        {body}
      </Link>
    );
  }
  if (onClick) {
    return (
      <button
        type="button"
        className={className}
        onClick={onClick}
        disabled={disabled}
        aria-current={selected || undefined}
      >
        {body}
      </button>
    );
  }
  return <div className={className}>{body}</div>;
}

/** Карточка-список для ListRow: строки через разделитель */
export function ListRowGroup({ children, label }: { children: ReactNode; label?: string }) {
  return (
    <div className={styles.group} role={label ? 'group' : undefined} aria-label={label}>
      {children}
    </div>
  );
}
