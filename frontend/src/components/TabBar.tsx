import { useRef } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useCapsule } from '../hooks/useCapsule';
import { useCurrentWorkout } from '../hooks/useWorkout';
import styles from './TabBar.module.css';

/** Иконки — простые линии 24×24 одним stroke, как колокольчик в шапке */
const ICONS = {
  home: 'M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z',
  workout: 'M3 10v4M6 7v10M18 7v10M21 10v4M6 12h12',
  history: 'M12 7v5l3 2M21 12a9 9 0 1 1-3-6.7M21 4v4h-4',
  records: 'M8 4h8v5a4 4 0 0 1-8 0zM8 6H5v1a3 3 0 0 0 3 3M16 6h3v1a3 3 0 0 1-3 3M12 13v4M8 20h8M10 17h4',
  profile: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 20a8 8 0 0 1 16 0',
} as const;

const TABS: { to: string; label: string; icon: keyof typeof ICONS }[] = [
  { to: '/', label: 'Главная', icon: 'home' },
  { to: '/history', label: 'История', icon: 'history' },
  { to: '/workout', label: 'Тренировка', icon: 'workout' },
  { to: '/records', label: 'Рекорды', icon: 'records' },
  { to: '/profile', label: 'Профиль', icon: 'profile' },
];

/**
 * Нижняя панель вкладок на телефоне: до разделов дотягивается большой палец, шапка не съедает экран.
 * "Тренировка" посередине — главное действие приложения. Идёт тренировка — на вкладке оранжевая точка.
 * На компьютере панель скрыта (CSS), там меню в шапке.
 *
 * Светлая капсула под выбранной вкладкой — отдельный элемент: при смене раздела она перетекает
 * к новой вкладке "каплей" (useCapsule), а подпись вкладки плавно раскрывается.
 */
export function TabBar() {
  const current = useCurrentWorkout();
  const inProgress = current.data != null;
  const { pathname } = useLocation();
  const navRef = useRef<HTMLElement>(null);
  const capsuleRef = useRef<HTMLSpanElement>(null);
  useCapsule(navRef, capsuleRef, pathname);

  return (
    <nav ref={navRef} aria-label="Разделы" className={styles.bar}>
      <span ref={capsuleRef} className={styles.capsule} aria-hidden="true" />
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/'}
          className={({ isActive }) => [styles.tab, isActive && styles.active].filter(Boolean).join(' ')}
        >
          <span className={styles.icon}>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d={ICONS[tab.icon]} />
            </svg>
            {tab.icon === 'workout' && inProgress && <span className={styles.dot} />}
          </span>
          <span className={styles.label}>
            <span className={styles.labelText}>{tab.label}</span>
          </span>
          {tab.icon === 'workout' && inProgress && <span className="visually-hidden">, идёт тренировка</span>}
        </NavLink>
      ))}
    </nav>
  );
}
