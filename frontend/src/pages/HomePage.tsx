import { useEffect, useState } from 'react';
import { authApi, type UserResponse } from '../api/auth';
import { Card } from '../components/ui/Card';
import styles from './HomePage.module.css';

/** Пока заглушка: проверяем цепочку фронт -> gateway -> user-service. Настоящая главная — следующая ветка. */
export function HomePage() {
  const [user, setUser] = useState<UserResponse | null>(null);

  useEffect(() => {
    // флаг на случай, если компонент исчезнет раньше, чем придёт ответ
    let active = true;
    authApi
      .me()
      .then((me) => active && setUser(me))
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className={styles.page}>
      <div>
        <p className={styles.greeting}>Привет, {user?.displayName || user?.email || '…'}</p>
        <h1 className={styles.title}>Главная</h1>
      </div>
      <Card variant="inverse">
        <h2 className={styles.cardTitle}>Каркас готов</h2>
        <p className={styles.muted}>Вход работает, данные идут через gateway. Скоро здесь будет ваш прогресс.</p>
      </Card>
    </div>
  );
}
