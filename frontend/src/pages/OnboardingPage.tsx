import { useQuery } from '@tanstack/react-query';
import { Navigate, useNavigate } from 'react-router-dom';
import { usersApi } from '../api/users';
import { ProfileForm } from '../components/profile/ProfileForm';
import { Card } from '../components/ui/Card';
import styles from './AuthPage.module.css';

/**
 * Онбординг сразу после регистрации: пол, уровень, год рождения, рост, вес.
 * Без него аналитика берёт средний уровень по умолчанию, и "норма" на графиках неточная.
 * Вне общего меню (как вход и регистрация): один экран — одно действие.
 */
export function OnboardingPage() {
  const navigate = useNavigate();
  const profile = useQuery({ queryKey: ['profile'], queryFn: usersApi.profile });

  if (profile.data?.onboardingCompleted) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className={styles.page}>
      <div className={styles.box}>
        <span className={styles.logo}>VEGAS</span>
        <Card size="section">
          <h1 className={styles.title}>Расскажите о себе</h1>
          <p className={styles.switch}>Займёт минуту. Данные можно поменять в профиле.</p>
          {profile.isPending ? (
            <p className={styles.switch}>Загружаем…</p>
          ) : (
            <ProfileForm profile={profile.data} submitLabel="Готово" onSaved={() => navigate('/', { replace: true })} />
          )}
        </Card>
      </div>
    </div>
  );
}
