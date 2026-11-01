import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { useAuth } from '../../auth/AuthProvider';
import { demoTimeLeft } from '../../lib/demo';
import styles from './Account.module.css';

/**
 * Полоса "ДЕМО · удалится через 23 ч · Зарегистрироваться" над всеми экранами демо (docs/demo.md).
 * Регистрация — с нуля: данные демо ненастоящие, переносить их некуда.
 */
export function DemoBanner() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  if (!me.data?.demo || !me.data.demoExpiresAt) {
    return null;
  }

  const register = () => {
    // сначала на регистрацию, потом выход: наоборот закрытая страница успела бы сама увести на /login
    navigate('/register', { replace: true, state: { fromDemo: true } });
    void logout();
  };

  return (
    <div className={styles.demoBanner} role="status">
      <span className={styles.demoBadge}>ДЕМО</span>
      <span className={styles.demoText}>удалится через {demoTimeLeft(me.data.demoExpiresAt)}</span>
      <button type="button" className={styles.demoRegister} onClick={register}>
        Зарегистрироваться
      </button>
    </div>
  );
}
