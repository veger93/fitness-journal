import type { WorkoutPlanProgress } from '../../lib/setControl';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import styles from './FinishPlanDialog.module.css';

/**
 * «Завершить», а план дня программы выполнен не весь (макет S3): что осталось и два пути — доделать или
 * завершить как есть. Без упрёка: тренировка сохранится в любом случае.
 */
export function FinishPlanDialog({
  progress,
  busy,
  onContinue,
  onFinish,
  onClose,
}: {
  progress: WorkoutPlanProgress;
  busy: boolean;
  onContinue: () => void;
  onFinish: () => void;
  onClose: () => void;
}) {
  const left = progress.total - progress.done;
  // кольцо: длина окружности r=27 — 2πr ≈ 169.6
  const circumference = 2 * Math.PI * 27;
  const filled = (progress.done / progress.total) * circumference;

  return (
    <Dialog open onClose={onClose} title="Почти всё!">
      <div className={styles.body}>
        <div className={styles.summary}>
          <svg
            width="72"
            height="72"
            viewBox="0 0 64 64"
            role="img"
            aria-label={`Сделано ${progress.done} из ${progress.total} подходов`}
          >
            <circle cx="32" cy="32" r="27" className={styles.track} />
            <circle
              cx="32"
              cy="32"
              r="27"
              className={styles.fill}
              strokeDasharray={`${filled} ${circumference}`}
              transform="rotate(-90 32 32)"
            />
            <text x="32" y="37" textAnchor="middle" className={styles.ringText}>
              {progress.done}/{progress.total}
            </text>
          </svg>
          <p className={styles.muted}>
            До плана дня осталось {left} {left === 1 ? 'подход' : left < 5 ? 'подхода' : 'подходов'}.
          </p>
        </div>

        <ul className={styles.list}>
          {progress.remaining.map((r, i) => (
            <li key={r.id} className={styles.row}>
              <span className={styles.mark}>{i + 1}</span>
              <span className={styles.name}>{r.name}</span>
              <span className={styles.muted}>
                {r.done} из {r.planned}
              </span>
            </li>
          ))}
        </ul>

        <Button fullWidth onClick={onContinue}>
          Доделать
        </Button>
        <Button variant="secondary" fullWidth disabled={busy} onClick={onFinish}>
          {busy ? 'Завершаем…' : 'Завершить так'}
        </Button>
        <p className={styles.note}>Ничего страшного: тренировка сохранится как есть.</p>
      </div>
    </Dialog>
  );
}
