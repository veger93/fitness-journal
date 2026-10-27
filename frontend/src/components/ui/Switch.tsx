import { useId } from 'react';
import styles from './Switch.module.css';

/**
 * Переключатель вкл/выкл. Внутри — обычный checkbox с role="switch": дикторы говорят "включено / выключено",
 * пробел переключает, а вся строка с подписью кликабельна.
 */
export function Switch({
  label,
  hint,
  checked,
  onChange,
}: {
  label: string;
  hint?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  const id = useId();
  return (
    <label htmlFor={id} className={styles.row}>
      <span className={styles.text}>
        <span className={styles.label}>{label}</span>
        {hint && <span className={styles.hint}>{hint}</span>}
      </span>
      <input
        id={id}
        type="checkbox"
        role="switch"
        className={styles.input}
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      <span className={styles.track} aria-hidden="true" />
    </label>
  );
}
