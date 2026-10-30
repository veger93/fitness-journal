import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { authApi, type UserResponse } from '../../api/auth';
import { useActingClient } from '../../trainer/ActingClientProvider';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Dialog } from '../ui/Dialog';
import { TextField } from '../ui/TextField';
import styles from './Account.module.css';

/**
 * "Подтвердите email" (docs/account.md): приложением можно пользоваться и без этого — просто напоминаем.
 * Без подтверждённого email не сработает "Забыли пароль?". Ошибся в адресе — "Исправить email".
 * Показывается на главной и в профиле, пока email не подтверждён; в режиме тренера за клиента — нет.
 */
export function EmailVerifyCard() {
  const queryClient = useQueryClient();
  const { client: acting } = useActingClient();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const [code, setCode] = useState('');
  const [sentAgain, setSentAgain] = useState(false);
  const [changing, setChanging] = useState(false);

  const onUser = (user: UserResponse) => queryClient.setQueryData(['me'], user);
  const confirm = useMutation({ mutationFn: () => authApi.confirmEmail(code), onSuccess: onUser });
  const resend = useMutation({ mutationFn: authApi.resendEmailCode, onSuccess: () => setSentAgain(true) });

  if (acting || !me.data?.email || me.data.emailVerified) {
    return null;
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    confirm.mutate();
  };

  return (
    <Card>
      <form className={styles.verify} onSubmit={onSubmit}>
        <div>
          <h2 className={styles.title}>Подтвердите email</h2>
          <p className={styles.muted}>
            Отправили код на <b>{me.data.email}</b>. Он нужен, чтобы восстановить пароль, если забудете.
          </p>
        </div>
        <div className={styles.codeRow}>
          <TextField
            label="Код из письма"
            inputMode="numeric"
            autoComplete="one-time-code"
            pattern="\d{6}"
            maxLength={6}
            placeholder="000000"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
          />
          <Button type="submit" disabled={code.length !== 6 || confirm.isPending}>
            Подтвердить
          </Button>
        </div>
        {confirm.isError && <p className={styles.error}>{confirm.error.message}</p>}
        {resend.isError && <p className={styles.error}>{resend.error.message}</p>}
        <div className={styles.links}>
          <button type="button" className={styles.link} onClick={() => resend.mutate()} disabled={resend.isPending}>
            {sentAgain ? 'Отправили ещё раз ✓' : 'Отправить код ещё раз'}
          </button>
          <button type="button" className={styles.link} onClick={() => setChanging(true)}>
            Исправить email
          </button>
        </div>
      </form>
      <ChangeEmailDialog
        open={changing}
        onClose={() => setChanging(false)}
        onChanged={(user) => {
          onUser(user);
          setCode('');
          setSentAgain(false);
          setChanging(false);
        }}
      />
    </Card>
  );
}

/** Новый email + пароль (чтобы чужой с открытого телефона не увёл аккаунт). На новый адрес сразу уходит код */
function ChangeEmailDialog({
  open,
  onClose,
  onChanged,
}: {
  open: boolean;
  onClose: () => void;
  onChanged: (user: UserResponse) => void;
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const change = useMutation({ mutationFn: () => authApi.changeEmail(email, password), onSuccess: onChanged });

  return (
    <Dialog open={open} onClose={onClose} title="Исправить email">
      <form
        className={styles.verify}
        onSubmit={(event) => {
          event.preventDefault();
          change.mutate();
        }}
      >
        <TextField
          label="Новый email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <TextField
          label="Пароль"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {change.isError && <p className={styles.error}>{change.error.message}</p>}
        <Button type="submit" fullWidth disabled={change.isPending}>
          Сохранить и прислать код
        </Button>
      </form>
    </Dialog>
  );
}
