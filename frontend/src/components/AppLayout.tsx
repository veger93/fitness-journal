import { Link, NavLink, Outlet } from 'react-router-dom';
import { EmailVeil } from './account/EmailVeil';
import { NotificationBell } from './NotificationBell';
import { ClientBand } from './trainer/ClientBand';
import { ClientReminder } from './trainer/ClientReminder';
import { WhoSwitcher } from './trainer/WhoSwitcher';
import { TabBar } from './TabBar';
import styles from './AppLayout.module.css';

const NAV = [
  { to: '/', label: 'Главная' },
  { to: '/workout', label: 'Тренировка' },
  { to: '/history', label: 'История' },
  { to: '/records', label: 'Рекорды' },
  { to: '/profile', label: 'Профиль' },
  // Комплексы — появятся вместе со своим экраном
];

/**
 * Общая "рамка" всех страниц после входа: шапка + контент.
 * На телефоне меню из шапки уходит в нижнюю панель вкладок (TabBar), в шапке остаются логотип и колокольчик.
 */
export function AppLayout() {
  return (
    <div className={styles.page}>
      {/* email не подтверждён — тёмная вуаль с намёком сверху главной (сама решает, показываться ли) */}
      <EmailVeil />
      <div className={styles.container}>
        {/* тренер работает за клиента — оранжевая полоса с именем (режим тренера, docs/trainer.md) */}
        <ClientBand />
        <header className={styles.header}>
          <Link to="/" className={styles.logo}>
            VEGAS
          </Link>
          <nav aria-label="Разделы" className={styles.nav}>
            {NAV.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => [styles.link, isActive && styles.active].filter(Boolean).join(' ')}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className={styles.headerEnd}>
            <WhoSwitcher />
            <NotificationBell />
          </div>
        </header>
        <main>
          <Outlet />
        </main>
      </div>
      <TabBar />
      <ClientReminder />
    </div>
  );
}
