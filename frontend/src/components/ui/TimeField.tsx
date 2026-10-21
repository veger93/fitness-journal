import { useId } from 'react';
import styles from './TimeField.module.css';

interface TimeFieldProps {
  legend: string;
  /** значение в секундах */
  seconds: number;
  onChange: (seconds: number) => void;
}

/**
 * Время "минуты : секунды". Человек думает "26 минут 40 секунд", а не "1600 секунд".
 * Часы не делаем отдельным полем: 90 минут — это просто 90 в минутах.
 */
export function TimeField({ legend, seconds, onChange }: TimeFieldProps) {
  const labelId = useId();
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  const set = (m: number, s: number) => onChange(Math.max(0, m) * 60 + Math.min(59, Math.max(0, s)));

  return (
    // role="group" + подпись вместо <fieldset>/<legend>: legend браузеры прибивают к левому краю рамки
    <div role="group" aria-labelledby={labelId} className={styles.field}>
      <span id={labelId} className={styles.legend}>
        {legend}
      </span>
      <div className={styles.row}>
        <input
          className={styles.input}
          type="number"
          inputMode="numeric"
          min={0}
          aria-label={`${legend}: минуты`}
          value={minutes}
          onChange={(e) => set(Number(e.target.value || 0), rest)}
        />
        <span className={styles.colon} aria-hidden="true">
          :
        </span>
        <input
          className={styles.input}
          type="number"
          inputMode="numeric"
          min={0}
          max={59}
          aria-label={`${legend}: секунды`}
          value={String(rest).padStart(2, '0')}
          onChange={(e) => set(minutes, Number(e.target.value || 0))}
        />
      </div>
      <div className={styles.units} aria-hidden="true">
        <span>мин</span>
        <span>сек</span>
      </div>
    </div>
  );
}
