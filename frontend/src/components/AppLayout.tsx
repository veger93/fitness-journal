import { Link, NavLink, Outlet } from 'react-router-dom';
import { NotificationBell } from './NotificationBell';
import styles from './AppLayout.module.css';

const NAV = [
  { to: '/', label: 'Главная' },
  { to: '/workout', label: 'Тренировка' },
  { to: '/history', label: 'История' },
  { to: '/records', label: 'Рекорды' },
  { to: '/profile', label: 'Профиль' },
  // Комплексы — появятся вместе со своим экраном
];

/** Общая "рамка" всех страниц после входа: шапка + контент. */
export function AppLayout() {
  return (
    <div className={styles.page}>
      <div className={styles.container}>
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
          <NotificationBell />
        </header>
        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
