import { useNavigate } from 'react-router-dom';
import { useClients, useClientsInProgress } from '../../hooks/useTrainer';
import { workoutStatusText } from '../../lib/trainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import styles from './Trainer.module.css';

/**
 * Мягкое напоминание вместо блокировки (docs/trainer.md): тренер в своём дневнике, а у клиента
 * не завершена тренировка (идёт или подготовлена) — плашка над нижней панелью, тап возвращает к клиенту на экран тренировки.
 */
export function ClientReminder() {
  const { client: acting, actAs } = useActingClient();
  const clients = useClients();
  const inProgress = useClientsInProgress(clients.data ?? []);
  const navigate = useNavigate();

  if (acting) {
    return null;
  }
  const first = clients.data?.find((client) => inProgress.has(client.id));
  const workout = first && inProgress.get(first.id);
  if (!first || !workout) {
    return null;
  }
  const others = inProgress.size - 1;

  return (
    <button
      type="button"
      className={styles.reminder}
      onClick={() => {
        actAs(first);
        navigate('/workout/active');
      }}
    >
      <span className={styles.reminderDot} aria-hidden="true" />
      <span className={styles.reminderText}>
        {/* "Иван: идёт…", а не "У Иван идёт…": склонять имена автоматически не умеем */}
        <b>{first.displayName}</b>: тренировка {workoutStatusText(workout.startedAt)}
        {others > 0 && ` (и ещё ${others})`}
      </span>
      <span className={styles.reminderGo}>Вернуться →</span>
    </button>
  );
}
