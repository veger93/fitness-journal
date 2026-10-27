import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { notificationsApi, type AppNotification } from '../api/notifications';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { NOTIFICATION_LABELS, toFeedItem } from '../lib/notifications';
import styles from './NotificationsPage.module.css';

const dateTime = new Intl.DateTimeFormat('ru-RU', {
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
});

/**
 * Лента уведомлений: рекорды, плато, перегрузки, итоги месяца, напоминания — то же, что приходит письмом.
 * Открыл ленту — всё прочитано: отмечаем на сервере, колокольчик гаснет.
 * Новые в этот раз всё равно выделены: признак "read" взят из ответа ДО отметки.
 */
export function NotificationsPage() {
  const queryClient = useQueryClient();
  const list = useQuery({ queryKey: ['notifications', 'list'], queryFn: () => notificationsApi.list() });
  const markRead = useMutation({
    mutationFn: notificationsApi.markAllRead,
    // обновляем только счётчик: список не перезапрашиваем, чтобы выделение новых не пропало у пользователя на глазах
    onSuccess: () => queryClient.setQueryData(['notifications', 'unread'], { count: 0 }),
  });

  // отметить один раз за открытие страницы, когда в загруженной ленте есть непрочитанные
  const marked = useRef(false);
  const hasUnread = list.data?.content.some((n) => !n.read) ?? false;
  useEffect(() => {
    if (hasUnread && !marked.current) {
      marked.current = true;
      markRead.mutate();
    }
  }, [hasUnread, markRead]);

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Уведомления</h1>
      <p className={styles.muted}>
        Рекорды, плато, перегрузки и итоги месяца — то же, что приходит на почту. Напоминания настраиваются в{' '}
        <Link to="/profile">профиле</Link>.
      </p>
      {list.isPending && <p className={styles.muted}>Загружаем…</p>}
      {list.isError && <p className={styles.muted}>{list.error.message}</p>}
      {list.data?.content.length === 0 && (
        <Card variant="inverse" size="section">
          <h2 className={styles.cardTitle}>Пока тихо</h2>
          <p className={styles.onInverse}>Здесь появятся новые рекорды, подсказки при плато и итоги месяца.</p>
        </Card>
      )}
      {list.data && list.data.content.length > 0 && (
        <ul className={styles.list}>
          {list.data.content.map((n) => (
            <NotificationItem key={n.id} notification={n} />
          ))}
        </ul>
      )}
    </div>
  );
}

function NotificationItem({ notification }: { notification: AppNotification }) {
  const item = toFeedItem(notification);
  const accent = notification.type === 'OVERLOAD_RISK';
  return (
    <li className={`${styles.item} ${notification.read ? '' : styles.unread}`}>
      <div className={styles.head}>
        <Badge variant={accent ? 'accent' : notification.type === 'PERSONAL_RECORD' ? 'inverse' : 'neutral'}>
          {NOTIFICATION_LABELS[notification.type]}
        </Badge>
        <span className={styles.muted}>{dateTime.format(new Date(notification.createdAt))}</span>
        {!notification.read && <span className={styles.newMark}>новое</span>}
      </div>
      <h2 className={styles.subject}>{notification.subject}</h2>
      {/* текст письма с переносами строк: white-space: pre-line в CSS */}
      <p className={styles.body}>{item.text}</p>
      {item.link && (
        <Link to={item.link.to} className={styles.action}>
          {item.link.label} →
        </Link>
      )}
    </li>
  );
}
