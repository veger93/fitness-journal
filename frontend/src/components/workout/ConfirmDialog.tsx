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
}

/** Подтверждение необратимого действия. Вместо window.confirm — он некрасивый и блокирует страницу. */
export function ConfirmDialog({ title, text, confirmLabel, busy, onConfirm, onClose }: ConfirmDialogProps) {
  return (
    <Dialog open onClose={onClose} title={title}>
      <p className={styles.center}>{text}</p>
      <Button fullWidth onClick={onConfirm} disabled={busy}>
        {confirmLabel}
      </Button>
      <Button variant="secondary" fullWidth onClick={onClose}>
        Отмена
      </Button>
    </Dialog>
  );
}
