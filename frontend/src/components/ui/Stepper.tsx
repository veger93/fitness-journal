import { useId } from 'react';
import styles from './Stepper.module.css';

interface StepperProps {
  label: string;
  value: number;
  onChange: (value: number) => void;
  step: number;
  min?: number;
  max?: number;
  /** "decimal" — вес (2,5 кг), "numeric" — целые (повторы) */
  inputMode?: 'decimal' | 'numeric';
}

/** Число с кнопками − / +. Можно и вписать руками: в зале удобнее кнопки, дома — клавиатура. */
export function Stepper({ label, value, onChange, step, min = 0, max = 100_000, inputMode = 'numeric' }: StepperProps) {
  const id = useId();
  const clamp = (v: number) => Math.min(max, Math.max(min, Math.round(v * 100) / 100));

  return (
    <div className={styles.stepper}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <div className={styles.row}>
        <button
          type="button"
          className={styles.button}
          aria-label={`${label}: уменьшить на ${step}`}
          onClick={() => onChange(clamp(value - step))}
        >
          −
        </button>
        <input
          id={id}
          className={styles.input}
          type="number"
          inputMode={inputMode}
          min={min}
          max={max}
          // step="any": шаг только для кнопок ± . Иначе браузер счёл бы 60 сек "неверным" при min=1, step=5
          step="any"
          value={Number.isNaN(value) ? '' : value}
          onChange={(e) => onChange(e.target.value === '' ? Number.NaN : clamp(Number(e.target.value)))}
        />
        <button
          type="button"
          className={styles.button}
          aria-label={`${label}: увеличить на ${step}`}
          onClick={() => onChange(clamp((Number.isNaN(value) ? 0 : value) + step))}
        >
          +
        </button>
      </div>
    </div>
  );
}
