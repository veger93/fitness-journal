import { Link, NavLink, Outlet } from 'react-router-dom';
import styles from './AppLayout.module.css';

const NAV = [
  { to: '/', label: 'Главная' },
  { to: '/profile', label: 'Профиль' },
  // Комплексы, История, Рекорды — появятся вместе со своими экранами
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
                end
                className={({ isActive }) => [styles.link, isActive && styles.active].filter(Boolean).join(' ')}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </header>
        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
