import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState, type FormEvent } from 'react';
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
 * Живёт в профиле, пока email не подтверждён (на главной — только намёк EmailVeil); в режиме тренера за клиента — нет.
 */
export function EmailVerifyCard() {
  const queryClient = useQueryClient();
  const { client: acting } = useActingClient();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const [code, setCode] = useState('');
  const [sentAgain, setSentAgain] = useState(false);
  const [changing, setChanging] = useState(false);
  // подтвердили — сначала показываем "Email подтверждён ✓", и только потом обновляем пользователя (карточка уйдёт)
  const [confirmed, setConfirmed] = useState<UserResponse | null>(null);

  const onUser = (user: UserResponse) => queryClient.setQueryData(['me'], user);
  const confirm = useMutation({ mutationFn: () => authApi.confirmEmail(code), onSuccess: setConfirmed });
  const resend = useMutation({ mutationFn: authApi.resendEmailCode, onSuccess: () => setSentAgain(true) });

  if (acting || !me.data?.email || me.data.emailVerified) {
    return null;
  }

  if (confirmed) {
    return <EmailConfirmed onDone={() => onUser(confirmed)} />;
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

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/**
 * На месте карточки: "Email подтверждён ✓" на лёгком зелёном свечении. Галочка "пишется" росчерком:
 * короткий штрих вниз, микропауза в нижней точке, взмах вверх. Через ~2 с всё растворяется, блок схлопывается
 * (карточки ниже плавно поднимаются) — и только тогда onDone убирает компонент.
 */
function EmailConfirmed({ onDone }: { onDone: () => void }) {
  const box = useRef<HTMLDivElement>(null);
  const tick = useRef<SVGPathElement>(null);
  // onDone в ref: эффект запускаем один раз, а не на каждый рендер родителя
  const done = useRef(onDone);
  useEffect(() => {
    done.current = onDone;
  }, [onDone]);

  useEffect(() => {
    const el = box.current;
    const path = tick.current;
    // без анимаций (настройка "уменьшить движение" или тесты в jsdom) — просто показываем надпись 1.5 с
    if (!el || !path || typeof el.animate !== 'function' || prefersReducedMotion()) {
      const timer = setTimeout(() => done.current(), 1500);
      return () => clearTimeout(timer);
    }
    const out = 'cubic-bezier(0.16, 1, 0.3, 1)';
    const animations = [
      el.animate(
        [
          { opacity: 0, transform: 'translateY(8px) scale(.97)' },
          { opacity: 1, transform: 'none' },
        ],
        {
          duration: 420,
          easing: out,
          fill: 'both',
        },
      ),
      path.animate(
        [
          { strokeDashoffset: 22, easing: 'cubic-bezier(.3,.6,.5,1)' },
          { strokeDashoffset: 15.6, offset: 0.32 },
          { strokeDashoffset: 15.6, offset: 0.42, easing: 'cubic-bezier(.5,0,.3,1)' },
          { strokeDashoffset: 0 },
        ],
        { duration: 520, delay: 380, fill: 'both' },
      ),
    ];
    const fade = el.animate([{ opacity: 1 }, { opacity: 0, transform: 'scale(1.04)' }], {
      duration: 450,
      delay: 2200,
      easing: 'ease-in',
      fill: 'forwards',
    });
    // надпись растворилась — схлопываем высоту, чтобы карточки ниже поднялись плавно, а не прыжком
    fade.onfinish = () => {
      const collapse = el.animate([{ height: `${el.offsetHeight}px` }, { height: '0px', marginBottom: '0px' }], {
        duration: 400,
        easing: out,
        fill: 'forwards',
      });
      collapse.onfinish = () => done.current();
      animations.push(collapse);
    };
    animations.push(fade);
    return () => animations.forEach((a) => a.cancel());
  }, []);

  return (
    <div ref={box} className={styles.confirmed} role="status">
      <span className={styles.confirmedText}>
        Email подтверждён
        <svg className={styles.tick} viewBox="0 0 24 24" aria-hidden="true">
          <path ref={tick} d="M4.5 12.8l4.2 4.4L19.5 6" />
        </svg>
      </span>
    </div>
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
