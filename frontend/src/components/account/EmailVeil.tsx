import { useQuery } from '@tanstack/react-query';
import { Link, useLocation } from 'react-router-dom';
import { analyticsApi } from '../../api/analytics';
import { authApi } from '../../api/auth';
import { useActingClient } from '../../trainer/ActingClientProvider';
import styles from './Account.module.css';

/**
 * Ненавязчивый намёк "подтвердите email" на главной (docs/account.md): сверху тёмная вуаль, которая растворяется
 * вниз, в ней мелкая белая ссылка в профиль — там карточка с полем для кода (EmailVerifyCard).
 *
 * Показываем не сразу после регистрации, а после первой записанной тренировки: сначала человек должен
 * попробовать приложение, а уже потом есть что терять. В режиме тренера за клиента — не показываем.
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

  if (!onHome || !unverified || dashboard.data?.empty !== false) {
    return null;
  }

  return (
    <>
      {/* сама вуаль — только картинка: клики проходят сквозь неё к шапке */}
      <div className={styles.veil} aria-hidden="true" />
      <Link to="/profile" className={styles.veilLink}>
        Подтвердите email, чтобы не потерять доступ →
      </Link>
    </>
  );
}
