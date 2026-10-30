import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { ApiError } from '../api/client';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { TextField } from '../components/ui/TextField';
import styles from './AuthPage.module.css';

/**
 * "Забыли пароль?" (docs/account.md) в два шага: email -> код из письма + новый пароль.
 * Код приходит только на подтверждённый email. Ответ на первом шаге всегда одинаковый —
 * по этой форме нельзя узнать, зарегистрирован ли человек в Vegas.
 */
export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'email' | 'code'>('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const run = async (action: () => Promise<void>) => {
    setError(null);
    setSubmitting(true);
    try {
      await action();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Что-то пошло не так');
    } finally {
      setSubmitting(false);
    }
  };

  const sendCode = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await authApi.forgotPassword(email);
      setStep('code');
    });
  };

  const reset = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await authApi.resetPassword({ email, code, newPassword: password });
      navigate('/login', { replace: true, state: { passwordChanged: true } });
    });
  };

  return (
    <div className={styles.page}>
      <div className={styles.box}>
        <span className={styles.logo}>VEGAS</span>
        <Card size="section">
          {step === 'email' ? (
            <form className={styles.form} onSubmit={sendCode}>
              <h1 className={styles.title}>Забыли пароль?</h1>
              <p className={styles.switch} style={{ textAlign: 'left' }}>
                Пришлём код на email, с которым вы входите. Код приходит, только если email был подтверждён.
              </p>
              <TextField
                label="Email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              {error && <p className={styles.error}>{error}</p>}
              <Button type="submit" fullWidth disabled={submitting}>
                Прислать код
              </Button>
            </form>
          ) : (
            <form className={styles.form} onSubmit={reset}>
              <h1 className={styles.title}>Новый пароль</h1>
              <p className={styles.switch} style={{ textAlign: 'left' }}>
                Если <b>{email}</b> есть в Vegas и подтверждён, на него пришёл код. Он действует 15 минут.
              </p>
              <TextField
                label="Код из письма"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                required
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              />
              <TextField
                label="Новый пароль"
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                maxLength={72}
                hint="От 8 символов. Все входы на других устройствах закроются"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              {error && <p className={styles.error}>{error}</p>}
              <Button type="submit" fullWidth disabled={submitting || code.length !== 6}>
                Сменить пароль
              </Button>
              <button type="button" className={styles.textButton} onClick={() => setStep('email')}>
                Не пришёл код? Отправить ещё раз
              </button>
            </form>
          )}
        </Card>
        <p className={styles.switch}>
          Вспомнили? <Link to="/login">Войти</Link>
        </p>
      </div>
    </div>
  );
}
