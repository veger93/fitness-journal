import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { notificationsApi, type AppNotification } from '../api/notifications';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { useIsTrainer } from '../hooks/useTrainer';
import { NOTIFICATION_LABELS, toFeedItem } from '../lib/notifications';
import { initial } from '../lib/trainer';
import { useActingClient } from '../trainer/ActingClientProvider';
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
  const isTrainer = useIsTrainer();
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
        <Link to="/profile">профиле</Link>.{isTrainer && ' О ваших клиентах — только здесь, без писем.'}
      </p>
      {list.isPending && <p className={styles.muted}>Загружаем…</p>}
      {list.isError && <p className={styles.muted}>{list.error.message}</p>}
      {list.data?.content.length === 0 && (
        <EmptyState title="Пока тихо" text="Здесь появятся новые рекорды, подсказки при плато и итоги месяца." />
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
  const { link, client } = item;
  const { actAs } = useActingClient();
  const navigate = useNavigate();
  const accent = notification.type === 'OVERLOAD_RISK';
  return (
    <li className={`${styles.item} ${notification.read ? '' : styles.unread}`}>
      <div className={styles.head}>
        {/* о клиенте — кружок с буквой, как в списке "Чей дневник открыть" */}
        {client && (
          <span className={styles.clientAvatar} aria-hidden="true">
            {initial(client.displayName)}
          </span>
        )}
        <Badge variant={accent ? 'accent' : notification.type === 'PERSONAL_RECORD' ? 'inverse' : 'neutral'}>
          {NOTIFICATION_LABELS[notification.type]}
        </Badge>
        <span className={styles.muted}>{dateTime.format(new Date(notification.createdAt))}</span>
        {!notification.read && <span className={styles.newMark}>новое</span>}
      </div>
      <h2 className={styles.subject}>{notification.subject}</h2>
      {/* текст письма с переносами строк: white-space: pre-line в CSS */}
      <p className={styles.body}>{item.text}</p>
      {link &&
        (client ? (
          <button
            type="button"
            className={styles.action}
            onClick={() => {
              actAs(client);
              navigate(link.to);
            }}
          >
            {link.label} →
          </button>
        ) : (
          <Link to={link.to} className={styles.action}>
            {link.label} →
          </Link>
        ))}
    </li>
  );
}
