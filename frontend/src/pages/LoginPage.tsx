import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthProvider';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { TextField } from '../components/ui/TextField';
import styles from './AuthPage.module.css';

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { from?: string; passwordChanged?: boolean; accountDeleted?: boolean } | null;
  const from = state?.from ?? '/';
  // пришли со страницы "Забыли пароль?" после смены пароля
  const passwordChanged = state?.passwordChanged ?? false;
  // пришли из профиля после "Удалить аккаунт"
  const accountDeleted = state?.accountDeleted ?? false;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // accountDeleted: сюда приходят из профиля ещё "вошедшими" — сессия закрывается сразу следом (DeleteAccountCard)
  if (isAuthenticated && !accountDeleted) {
    return <Navigate to={from} replace />;
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault(); // иначе браузер перезагрузит страницу, отправив форму сам
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Что-то пошло не так');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.box}>
        <span className={styles.logo}>VEGAS</span>
        <Card size="section">
          <form className={styles.form} onSubmit={onSubmit}>
            <h1 className={styles.title}>Вход</h1>
            {passwordChanged && <p className={styles.notice}>Пароль изменён — войдите с новым паролем.</p>}
            {accountDeleted && <p className={styles.notice}>Аккаунт и все данные удалены.</p>}
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
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {error && (
              <p className={styles.error} role="alert">
                {error}
              </p>
            )}
            <Button type="submit" fullWidth disabled={submitting}>
              {submitting ? 'Входим…' : 'Войти'}
            </Button>
            <Link to="/forgot-password" className={styles.textButton}>
              Забыли пароль?
            </Link>
          </form>
        </Card>
        <p className={styles.switch}>
          Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
        </p>
      </div>
    </div>
  );
}
