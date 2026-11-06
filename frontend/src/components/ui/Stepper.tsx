import { useEffect, useId, useState } from 'react';
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
  /** сколько знаков после запятой оставлять (0 — только целые) */
  decimals?: number;
}

/** 79.2 -> "79,2": дробь через запятую, как во всём приложении; NaN (поле очищено) -> "" */
export function formatStepperValue(value: number): string {
  return Number.isNaN(value) ? '' : String(value).replace('.', ',');
}

/**
 * Что человек впечатал: "79,2" и "79.2" -> 79.2, "" -> NaN (поле пустое), "79,2,1" или "abc" -> null (не принимаем).
 * decimals = 0 — только целые.
 */
export function parseStepperInput(text: string, decimals: number): number | null {
  const pattern = decimals > 0 ? /^\d*([.,]\d*)?$/ : /^\d*$/;
  if (!pattern.test(text)) {
    return null;
  }
  if (text === '' || text === ',' || text === '.') {
    return Number.NaN;
  }
  return Number(text.replace(',', '.'));
}

/**
 * Число с кнопками − / +. Можно и вписать руками: в зале удобнее кнопки, дома — клавиатура.
 * Поле — текстовое (type="text" + inputMode): у type="number" браузер сам решает, точка или запятая,
 * и часто показывает "79.2". Пока человек печатает, держим его текст как есть ("79," — ещё не дописал),
 * а в границы min/max приводим, когда он уходит из поля.
 */
export function Stepper({
  label,
  value,
  onChange,
  step,
  min = 0,
  max = 100_000,
  inputMode = 'numeric',
  decimals = 2,
}: StepperProps) {
  const id = useId();
  const factor = 10 ** decimals;
  const clamp = (v: number) => Math.min(max, Math.max(min, Math.round(v * factor) / factor));
  const [text, setText] = useState(() => formatStepperValue(value));

  // значение поменяли снаружи (кнопки ±, «Повторить прошлый») — показываем его.
  // Если текст и так означает это число ("79," при 79), не трогаем: человек ещё печатает
  useEffect(() => {
    const typed = parseStepperInput(text, decimals);
    const same = typed !== null && (typed === value || (Number.isNaN(typed) && Number.isNaN(value)));
    if (!same) {
      setText(formatStepperValue(value));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- реагируем только на новое значение снаружи
  }, [value]);

  return (
    <div className={styles.stepper}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <div className={styles.row}>
        <button
          type="button"
          className={styles.button}
          aria-label={`${label}: уменьшить на ${formatStepperValue(step)}`}
          onClick={() => onChange(clamp(value - step))}
        >
          −
        </button>
        <input
          id={id}
          className={styles.input}
          type="text"
          inputMode={decimals > 0 && inputMode === 'decimal' ? 'decimal' : 'numeric'}
          autoComplete="off"
          value={text}
          onChange={(e) => {
            const typed = parseStepperInput(e.target.value, inputMode === 'decimal' ? decimals : 0);
            if (typed === null) {
              return; // буква или вторая запятая — просто не печатаем
            }
            setText(e.target.value);
            onChange(typed);
          }}
          onBlur={() => {
            if (!Number.isNaN(value)) {
              const fixed = clamp(value);
              setText(formatStepperValue(fixed));
              if (fixed !== value) {
                onChange(fixed);
              }
            }
          }}
        />
        <button
          type="button"
          className={styles.button}
          aria-label={`${label}: увеличить на ${formatStepperValue(step)}`}
          onClick={() => onChange(clamp((Number.isNaN(value) ? 0 : value) + step))}
        >
          +
        </button>
      </div>
    </div>
  );
}
