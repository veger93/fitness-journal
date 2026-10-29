import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { authApi } from '../api/auth';
import { useAuth } from '../auth/AuthProvider';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ChoiceCards } from '../components/ui/ChoiceCards';
import { TextField } from '../components/ui/TextField';
import styles from './AuthPage.module.css';

type Continue = 'TRAINER' | 'ALONE';

/**
 * /claim/:token — клиент получает от тренера свой дневник (docs/trainer.md, шаг 4).
 * Открывается по ссылке или QR-коду с телефона тренера. Придумал email и пароль — аккаунт его и он сразу вошёл.
 */
export function ClaimPage() {
  const { token = '' } = useParams();
  const { isAuthenticated, claim, logout } = useAuth();
  const navigate = useNavigate();
  const preview = useQuery({ queryKey: ['claim', token], queryFn: () => authApi.claimPreview(token), retry: false });

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [next, setNext] = useState<Continue | null>(null);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // ссылку открыл уже вошедший человек (часто — сам тренер на своём телефоне)
  if (isAuthenticated && !submitting) {
    return (
      <Layout>
        <h1 className={styles.title}>Вы уже вошли</h1>
        <p className={styles.switch} style={{ textAlign: 'left' }}>
          Эту ссылку нужно открыть на телефоне клиента. Если это ваш дневник — выйдите и откройте ссылку снова.
        </p>
        <Button variant="secondary" fullWidth onClick={logout}>
          Выйти
        </Button>
      </Layout>
    );
  }

  if (preview.isPending) {
    return (
      <Layout>
        <p className={styles.switch}>Проверяем ссылку…</p>
      </Layout>
    );
  }
  if (preview.isError) {
    return (
      <Layout>
        <h1 className={styles.title}>Ссылка не работает</h1>
        <p className={styles.error}>{preview.error.message}</p>
        <Link to="/">На главную</Link>
      </Layout>
    );
  }

  const { clientName, trainerName } = preview.data;

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!next) {
      setError('Выберите, как тренироваться дальше');
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await claim(token, { email, password, keepTrainer: next === 'TRAINER', consent });
      // профиль клиента заполнен не до конца (пол, год рождения) — главная сама отправит на онбординг
      navigate('/', { replace: true });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Что-то пошло не так');
      setSubmitting(false);
    }
  };

  return (
    <Layout>
      <form className={styles.form} onSubmit={onSubmit}>
        <h1 className={styles.title}>{clientName ? `${clientName}, это ваш дневник` : 'Ваш дневник тренировок'}</h1>
        <p className={styles.switch} style={{ textAlign: 'left' }}>
          Тренер {trainerName ?? ''} передаёт вам все ваши тренировки, рекорды и графики. Придумайте email и пароль — и
          дневник будет открываться у вас.
        </p>
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <TextField
          label="Пароль"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          hint="От 8 символов"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <ChoiceCards
          legend="Как дальше"
          options={[
            {
              value: 'TRAINER',
              title: 'Продолжаю с тренером',
              description: 'Тренер по-прежнему видит и записывает мои тренировки',
            },
            {
              value: 'ALONE',
              title: 'Дальше сам',
              description: 'Тренер больше не видит мой дневник',
            },
          ]}
          value={next}
          onChange={setNext}
        />
        <label className={styles.consent}>
          <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
          <span>Согласен на хранение и обработку моих данных о тренировках в Vegas</span>
        </label>
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
        <Button type="submit" variant="accent" fullWidth disabled={submitting || !consent}>
          {submitting ? 'Сохраняем…' : 'Забрать дневник'}
        </Button>
        <p className="footnote">Можно отключить тренера позже — в профиле.</p>
      </form>
    </Layout>
  );
}

function Layout({ children }: { children: ReactNode }) {
  return (
    <div className={styles.page}>
      <div className={styles.box}>
        <span className={styles.logo}>VEGAS</span>
        <Card size="section">{children}</Card>
      </div>
    </div>
  );
}
