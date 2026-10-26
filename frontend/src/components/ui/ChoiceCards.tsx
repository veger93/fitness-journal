import { useId } from 'react';
import styles from './ChoiceCards.module.css';

interface Choice<T extends string> {
  value: T;
  title: string;
  description?: string;
}

interface ChoiceCardsProps<T extends string> {
  legend: string;
  options: Choice<T>[];
  /** null — ещё ничего не выбрано (в онбординге не подставляем значение за пользователя) */
  value: T | null;
  onChange: (value: T) => void;
}

/**
 * Выбор одного варианта карточками с пояснением (пол, уровень подготовки).
 * Внутри — настоящие radio: стрелки на клавиатуре и экранные дикторы работают без лишнего кода.
 */
export function ChoiceCards<T extends string>({ legend, options, value, onChange }: ChoiceCardsProps<T>) {
  const name = useId();
  return (
    <fieldset className={styles.group}>
      <legend className={styles.legend}>{legend}</legend>
      {/* два варианта — всегда рядом; три и больше на телефоне встают списком (см. CSS) */}
      <div className={`${styles.cards} ${options.length > 2 ? styles.many : ''}`}>
        {options.map((option) => (
          <label key={option.value} className={styles.card}>
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={option.value === value}
              onChange={() => onChange(option.value)}
            />
            <span className={styles.body}>
              <span className={styles.title}>{option.title}</span>
              {option.description && <span className={styles.description}>{option.description}</span>}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
