import { useMutation } from '@tanstack/react-query';
import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { useAuth } from '../../auth/AuthProvider';
import { useClients, useIsTrainer } from '../../hooks/useTrainer';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { TextField } from '../ui/TextField';
import styles from './Account.module.css';

/** Сколько секунд заблокирована кнопка во втором окне тренера — успеть прочитать список */
export const LOCK_SECONDS = 5;

/**
 * "Удалить аккаунт" внизу профиля (docs/account.md, шаг 2). Удаление нельзя отменить, поэтому:
 * - всем — окно с последствиями и паролем (чужой с открытого телефона не удалит);
 * - тренеру с клиентами без аккаунта — второе окно: кто удалится вместе с ним, кнопка заблокирована 5 секунд.
 *   Клиенты со своим аккаунтом остаются — у них просто пропадает тренер.
 */
export function DeleteAccountCard() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const isTrainer = useIsTrainer();
  const clients = useClients();
  // тренер, а список клиентов ещё не загружен — ждём: иначе второе окно со списком можно "проскочить"
  const clientsUnknown = isTrainer && !clients.data;
  const managed = (clients.data ?? []).filter((client) => client.managed);
  const ownAccounts = (clients.data?.length ?? 0) - managed.length;

  const [open, setOpen] = useState(false);
  const [step, setStep] = useState<'password' | 'clients'>('password');
  const [password, setPassword] = useState('');

  const remove = useMutation({
    mutationFn: () => authApi.deleteAccount(password),
    onSuccess: () => {
      // сначала уходим на вход, потом закрываем сессию. Наоборот нельзя: после logout() профиль сразу
      // перебрасывает на /login сам (RequireAuth) — и сообщение "Аккаунт удалён" теряется
      navigate('/login', { replace: true, state: { accountDeleted: true } });
      void logout(); // чистим кэш и токен в памяти (cookie сервер уже удалил)
    },
    // неверный пароль во втором окне — возвращаем к полю пароля, ошибка видна там
    onError: () => setStep('password'),
  });

  const close = () => {
    setOpen(false);
    setStep('password');
    setPassword('');
    remove.reset();
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    remove.reset();
    if (managed.length > 0) {
      setStep('clients');
    } else {
      remove.mutate();
    }
  };

  return (
    <>
      <button type="button" className={styles.deleteLink} onClick={() => setOpen(true)}>
        Удалить аккаунт
      </button>

      <Dialog
        open={open}
        onClose={close}
        title={step === 'password' ? 'Удалить аккаунт?' : 'Удалятся и клиенты'}
        onBack={step === 'clients' ? () => setStep('password') : undefined}
      >
        {step === 'password' ? (
          <form className={styles.verify} onSubmit={onSubmit}>
            <p className={styles.muted}>
              Удалятся все тренировки, рекорды, графики и настройки. <b>Восстановить их будет нельзя.</b>
            </p>
            <TextField
              label="Пароль"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {remove.isError && <p className={styles.error}>{remove.error.message}</p>}
            <div className={styles.dialogActions}>
              <Button type="submit" variant="danger" disabled={!password || clientsUnknown || remove.isPending}>
                {managed.length > 0 ? 'Дальше' : 'Удалить навсегда'}
              </Button>
              <Button variant="secondary" onClick={close}>
                Отмена
              </Button>
            </div>
          </form>
        ) : (
          <ClientsStep
            names={managed.map((client) => client.displayName)}
            ownAccounts={ownAccounts}
            pending={remove.isPending}
            onConfirm={() => remove.mutate()}
            onCancel={close}
          />
        )}
      </Dialog>
    </>
  );
}

/** Второе окно тренера: список клиентов без аккаунта и кнопка с обратным отсчётом */
function ClientsStep({
  names,
  ownAccounts,
  pending,
  onConfirm,
  onCancel,
}: {
  names: string[];
  ownAccounts: number;
  pending: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  // окно открылось — отсчёт с начала (компонент монтируется заново при каждом переходе на этот шаг)
  const [left, setLeft] = useState(LOCK_SECONDS);
  useEffect(() => {
    if (left <= 0) return;
    const timer = setTimeout(() => setLeft(left - 1), 1000);
    return () => clearTimeout(timer);
  }, [left]);

  return (
    <div className={styles.verify}>
      <p className={styles.muted}>
        У этих клиентов нет своего аккаунта — они удалятся вместе с вами, со всеми тренировками:
      </p>
      <ul className={styles.clientList}>
        {names.map((name, index) => (
          <li key={index}>{name}</li>
        ))}
      </ul>
      {ownAccounts > 0 && (
        <p className={styles.muted}>
          Клиенты со своим аккаунтом ({ownAccounts}) останутся — у них просто не будет тренера.
        </p>
      )}
      <p className={styles.muted}>Кто-то хочет вести дневник сам — сначала передайте ему аккаунт.</p>
      <div className={styles.dialogActions}>
        <Button variant="danger" disabled={left > 0 || pending} onClick={onConfirm}>
          {left > 0 ? `Удалить всё · ${left}` : 'Удалить всё'}
        </Button>
        <Button variant="secondary" onClick={onCancel}>
          Отмена
        </Button>
      </div>
    </div>
  );
}
