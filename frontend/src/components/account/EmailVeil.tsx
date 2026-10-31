import { useQuery } from '@tanstack/react-query';
import { Link, useLocation } from 'react-router-dom';
import { analyticsApi } from '../../api/analytics';
import { authApi } from '../../api/auth';
import { useClients } from '../../hooks/useTrainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import styles from './Account.module.css';

/**
 * Ненавязчивый намёк "подтвердите email" на главной (docs/account.md): сверху тёмная вуаль, которая растворяется
 * вниз, в ней мелкая белая ссылка в профиль — там карточка с полем для кода (EmailVerifyCard).
 *
 * Показываем не сразу после регистрации, а когда уже есть что терять: после первой своей тренировки
 * или — тренеру — когда появился первый клиент (тренер может вести только клиентов, а сам не записываться).
 * В режиме тренера за клиента — не показываем.
 */
export function EmailVeil() {
  const { pathname } = useLocation();
  const { client: acting } = useActingClient();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const onHome = pathname === '/';
  const unverified = !acting && !!me.data?.email && !me.data.emailVerified;
  // тот же ключ, что у главной, — запрос уже в кэше, второй раз в сеть не идём
  const dashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: analyticsApi.dashboard,
    enabled: onHome && unverified,
  });
  // список клиентов грузится только тренеру (и он тоже уже в кэше — для "Я ▾" в шапке)
  const clients = useClients();
  const hasWorkouts = dashboard.data?.empty === false;
  const hasClients = (clients.data?.length ?? 0) > 0;

  if (!onHome || !unverified || !(hasWorkouts || hasClients)) {
    return null;
  }

  return (
    <>
      {/* сама вуаль — только картинка: клики проходят сквозь неё к шапке */}
      <div className={styles.veil} aria-hidden="true" />
      <div className={styles.veilSpace} />
      <Link to="/profile" className={styles.veilLink}>
        Подтвердите email, чтобы не потерять доступ →
      </Link>
    </>
  );
}
