import { useId, type ReactNode } from 'react';
import styles from './Select.module.css';

/**
 * Выпадающий список в стиле приложения: «пилюля» высотой как кнопка, своя стрелка.
 * Внутри — обычный <select>: на телефоне открывается родной список системы (удобно пальцем),
 * работает с клавиатурой и экранными дикторами. Меняем только внешний вид.
 */
export function Select({
  label,
  value,
  onChange,
  children,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  children: ReactNode;
}) {
  const id = useId();
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <span className={styles.wrap}>
        <select id={id} className={styles.select} value={value} onChange={(e) => onChange(e.target.value)}>
          {children}
        </select>
      </span>
    </div>
  );
}
