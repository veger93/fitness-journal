import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { useQuery } from '@tanstack/react-query';
import { useClients, useClientSignals, useClientsInProgress, useIsTrainer } from '../../hooks/useTrainer';
import { initial, signalBadge, workoutStatusText } from '../../lib/trainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import { Badge } from '../ui/Badge';
import { Dialog } from '../ui/Dialog';
import { NewClientForm } from './NewClientForm';
import styles from './Trainer.module.css';

/**
 * Переключатель "чей дневник" в шапке (вариант A). Виден только тренеру.
 * Тап — окно со списком: я и клиенты с одной меткой — идёт тренировка, перегрузка, плато, давно не был, рекорд
 * (docs/trainer.md, шаг 5); там же "Новый клиент".
 */
export function WhoSwitcher() {
  const isTrainer = useIsTrainer();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const { client: acting, actAs } = useActingClient();
  const clients = useClients();
  const inProgress = useClientsInProgress(clients.data ?? []);
  const signals = useClientSignals();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  // окно-меню раскрывается прямо под этой кнопкой
  const buttonRef = useRef<HTMLButtonElement>(null);

  if (!isTrainer) {
    return null;
  }

  const close = () => {
    setOpen(false);
    setCreating(false);
  };
  const pick = (next: { id: string; displayName: string } | null) => {
    if (next?.id !== acting?.id) {
      actAs(next);
    }
    close();
    // у клиента идёт тренировка — сразу на её экран, иначе остаёмся на текущем
    if (next && inProgress.has(next.id)) {
      navigate('/workout/active');
    }
  };

  return (
    <>
      <button ref={buttonRef} type="button" className={styles.who} onClick={() => setOpen(true)} aria-haspopup="dialog">
        <span className={acting ? `${styles.avatar} ${styles.avatarClient}` : styles.avatar} aria-hidden="true">
          {initial(acting ? acting.displayName : me.data?.displayName)}
        </span>
        <span className={styles.whoName}>{acting ? acting.displayName : 'Я'}</span>
        <span aria-hidden="true">▾</span>
        <span className="visually-hidden">— сменить, чей дневник открыт</span>
      </button>

      <Dialog
        open={open}
        onClose={close}
        onBack={creating ? () => setCreating(false) : undefined}
        title={creating ? 'Новый клиент' : 'Чей дневник открыть'}
        anchorRef={buttonRef}
      >
        {creating ? (
          <NewClientForm onCreated={(client) => pick(client)} />
        ) : (
          <>
            {/* data-stagger: строки появляются по очереди, когда окно раскрывается (Dialog) */}
            <ul className={styles.list} data-stagger>
              <li>
                <button
                  type="button"
                  className={acting ? styles.row : `${styles.row} ${styles.rowCurrent}`}
                  onClick={() => pick(null)}
                >
                  <span className={styles.avatar}>{initial(me.data?.displayName)}</span>
                  <span className={styles.rowText}>
                    <b>Я</b>
                    <span className={styles.muted}>свои тренировки</span>
                  </span>
                </button>
              </li>
              {clients.data?.map((client) => {
                const workout = inProgress.get(client.id);
                const badge = signalBadge(signals.get(client.id));
                return (
                  <li key={client.id}>
                    <button
                      type="button"
                      className={acting?.id === client.id ? `${styles.row} ${styles.rowCurrent}` : styles.row}
                      onClick={() => pick(client)}
                    >
                      <span className={`${styles.avatar} ${styles.avatarClient}`}>{initial(client.displayName)}</span>
                      <span className={styles.rowText}>
                        <b>{client.displayName}</b>
                        <span className={styles.muted}>{client.managed ? 'дневник ведёте вы' : 'свой аккаунт'}</span>
                      </span>
                      {/* одна метка: идущая тренировка важнее всего, иначе — главный сигнал аналитики */}
                      {workout ? (
                        <span className={styles.signal}>{workoutStatusText(workout.startedAt)}</span>
                      ) : (
                        badge && <Badge variant={badge.variant}>{badge.text}</Badge>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
            {clients.isError && <p className={styles.error}>{clients.error.message}</p>}
            {clients.data?.length === 0 && <p className={styles.muted}>Клиентов пока нет — добавьте первого.</p>}
            <button type="button" className={styles.add} onClick={() => setCreating(true)}>
              + Новый клиент
            </button>
          </>
        )}
      </Dialog>
    </>
  );
}
