import { exerciseImageUrl } from '../lib/exercise';
import styles from './ExerciseImage.module.css';

interface ExerciseImageProps {
  /** null — картинки нет: показываем заглушку того же размера, чтобы строки списка не "прыгали" */
  imageKey: string | null;
  /** 1 и 2 — два положения движения */
  phase?: 1 | 2;
  /** thumb — миниатюра в списке, medium — у заголовка страницы, large — в карточке упражнения */
  size?: 'thumb' | 'medium' | 'large';
  /** подпись для скринридера; у миниатюры рядом уже есть название — тогда alt пустой */
  alt?: string;
}

/**
 * Иллюстрация упражнения в рамке фиксированного размера 4:3.
 * Картинки — SVG из public/exercises (чёрные линии на прозрачном фоне), в тёмной теме инвертируются в CSS.
 */
export function ExerciseImage({ imageKey, phase = 1, size = 'thumb', alt = '' }: ExerciseImageProps) {
  const frame = `${styles.frame} ${styles[size]}`;
  if (!imageKey) {
    return (
      <span className={frame} aria-hidden="true">
        <svg viewBox="0 0 24 24" className={styles.placeholderIcon}>
          {/* гантель */}
          <path d="M3 10v4M6 8v8M18 8v8M21 10v4M6 12h12" />
        </svg>
      </span>
    );
  }
  return (
    <span className={frame}>
      <img src={exerciseImageUrl(imageKey, phase)} alt={alt} loading="lazy" className={styles.img} />
    </span>
  );
}
