import { useEffect, useId, useRef, type ReactNode } from 'react';
import styles from './Dialog.module.css';

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  children: ReactNode;
}

/**
 * Модальное окно на встроенном <dialog>. showModal() даёт бесплатно то, что руками писать долго:
 * затемнение фона, фокус внутри окна, закрытие по Esc, недоступность страницы под окном.
 */
export function Dialog({ open, onClose, title, subtitle, children }: DialogProps) {
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
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
          <h2 id={titleId} className={styles.title}>
            {title}
          </h2>
          {children}
        </div>
      )}
    </dialog>
  );
}
