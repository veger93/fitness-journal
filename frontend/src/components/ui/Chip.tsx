import type { ButtonHTMLAttributes } from 'react';
import styles from './Chip.module.css';

interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  selected?: boolean;
}

/** Кнопка-"таблетка" для переключения (упражнения в тренировке). aria-pressed говорит дикторам, что выбрано. */
export function Chip({ selected = false, className, ...rest }: ChipProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      className={[styles.chip, selected && styles.selected, className].filter(Boolean).join(' ')}
      {...rest}
    />
  );
}
