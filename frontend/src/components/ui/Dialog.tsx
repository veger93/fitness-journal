import { useEffect, useId, useRef, type ReactNode } from 'react';
import styles from './Dialog.module.css';

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  /** есть — слева в шапке кнопка "←" (шаг назад внутри окна: из карточки к списку, из категории к плиткам) */
  onBack?: () => void;
  /** своё содержимое левого угла шапки вместо "←" (миниатюра упражнения в окне подхода) */
  leading?: ReactNode;
  children: ReactNode;
}

/**
 * Модальное окно на встроенном <dialog>. showModal() даёт бесплатно то, что руками писать долго:
 * затемнение фона, фокус внутри окна, закрытие по Esc, недоступность страницы под окном.
 * Esc на телефоне нет — поэтому в шапке всегда крестик.
 * Шапка — три колонки одинаковой ширины по краям: так заголовок остаётся по центру, что бы ни стояло слева.
 */
export function Dialog({ open, onClose, title, subtitle, onBack, leading, children }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) {
      return;
    }
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    // onClose срабатывает и на Esc — синхронизируем состояние React
    <dialog ref={ref} className={styles.dialog} onClose={onClose} aria-labelledby={titleId}>
      {open && (
        <div className={styles.body}>
          <div className={styles.header}>
            <div className={styles.side}>
              {leading ??
                (onBack && (
                  <button type="button" className={styles.iconButton} aria-label="Назад" onClick={onBack}>
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M15 6l-6 6 6 6" />
                    </svg>
                  </button>
                ))}
            </div>
            <div className={styles.heading}>
              {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
              <h2 id={titleId} className={styles.title}>
                {title}
              </h2>
            </div>
            <div className={`${styles.side} ${styles.sideEnd}`}>
              <button type="button" className={styles.iconButton} aria-label="Закрыть" onClick={onClose}>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            </div>
          </div>
          {children}
        </div>
      )}
    </dialog>
  );
}
