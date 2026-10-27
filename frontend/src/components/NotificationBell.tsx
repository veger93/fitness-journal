import { useQuery } from '@tanstack/react-query';
import { NavLink } from 'react-router-dom';
import { notificationsApi } from '../api/notifications';
import { badgeText } from '../lib/notifications';
import styles from './AppLayout.module.css';

/** Раз в минуту — достаточно: уведомления приходят после тренировки или по расписанию, не каждую секунду */
const REFRESH_MS = 60_000;

/** Колокольчик в шапке: ведёт в ленту, кружок — сколько непрочитанных. */
export function NotificationBell() {
  const unread = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: REFRESH_MS,
  });
  const count = unread.data?.count ?? 0;

  return (
    <NavLink
      to="/notifications"
      className={({ isActive }) => [styles.bell, isActive && styles.bellActive].filter(Boolean).join(' ')}
      // для дикторов: иконка без текста, поэтому подпись — в aria-label
      aria-label={count > 0 ? `Уведомления, непрочитанных: ${count}` : 'Уведомления'}
    >
      <svg
        aria-hidden="true"
        width="22"
        height="22"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      {count > 0 && <span className={styles.badge}>{badgeText(count)}</span>}
    </NavLink>
  );
}
