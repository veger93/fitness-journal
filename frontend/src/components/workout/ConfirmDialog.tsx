import type { ReactNode } from 'react';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import styles from './Workout.module.css';

interface ConfirmDialogProps {
  title: string;
  text: string;
  confirmLabel: string;
  busy: boolean;
  onConfirm: () => void;
  onClose: () => void;
  /** что-то ещё между текстом и кнопками (галочка «обновить комплекс») */
  children?: ReactNode;
}

/** Подтверждение необратимого действия. Вместо window.confirm — он некрасивый и блокирует страницу. */
export function ConfirmDialog({ title, text, confirmLabel, busy, onConfirm, onClose, children }: ConfirmDialogProps) {
  return (
    <Dialog open onClose={onClose} title={title}>
      <p className={styles.center}>{text}</p>
      {children}
      <Button fullWidth onClick={onConfirm} disabled={busy}>
        {confirmLabel}
      </Button>
      <Button variant="secondary" fullWidth onClick={onClose}>
        Отмена
      </Button>
    </Dialog>
  );
}
