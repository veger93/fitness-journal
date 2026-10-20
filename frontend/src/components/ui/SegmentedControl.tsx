import { useId, type CSSProperties } from 'react';
import styles from './SegmentedControl.module.css';

interface Option<T extends string> {
  value: T;
  label: string;
}

interface SegmentedControlProps<T extends string> {
  legend: string;
  options: Option<T>[];
  value: T;
  onChange: (value: T) => void;
}

/** Переключатель "один из нескольких" (тема, период графика...). */
export function SegmentedControl<T extends string>({ legend, options, value, onChange }: SegmentedControlProps<T>) {
  const name = useId();
  return (
    <fieldset className={styles.group} style={{ '--count': options.length } as CSSProperties}>
      <legend className="visually-hidden">{legend}</legend>
      {options.map((option) => (
        <label key={option.value} className={styles.option}>
          <input
            type="radio"
            name={name}
            value={option.value}
            checked={option.value === value}
            onChange={() => onChange(option.value)}
          />
          <span>{option.label}</span>
        </label>
      ))}
    </fieldset>
  );
}
