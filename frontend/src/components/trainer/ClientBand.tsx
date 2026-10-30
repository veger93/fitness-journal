import { initial } from '../../lib/trainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import styles from './Trainer.module.css';

/**
 * Оранжевая полоса сверху, пока тренер работает за клиента: перепутать, кому записываешь подход, невозможно.
 * "✕ К себе" — вернуться в свой дневник.
 */
export function ClientBand() {
  const { client, actAs } = useActingClient();
  if (!client) {
    return null;
  }
  return (
    <div className={styles.band} role="status">
      <span className={`${styles.avatar} ${styles.avatarOnAccent}`} aria-hidden="true">
        {initial(client.displayName)}
      </span>
      <span className={styles.bandText}>
        <b>{client.displayName}</b>
        <span>вы ведёте клиента</span>
      </span>
      <button type="button" className={styles.bandBack} onClick={() => actAs(null)}>
        ✕ К себе
      </button>
    </div>
  );
}
