import type { AppNotification, NotificationType } from '../api/notifications';

/** Подпись типа в ленте и бейдж: что за событие */
export const NOTIFICATION_LABELS: Record<NotificationType, string> = {
  PERSONAL_RECORD: 'Рекорд',
  PLATEAU: 'Плато',
  OVERLOAD_RISK: 'Перегрузка',
  MONTHLY_REPORT: 'Итоги месяца',
  WORKOUT_REMINDER: 'Напоминание',
  INACTIVITY: 'Давно не было тренировок',
};

const SIGNATURE = /\n+—\nVegas, дневник тренировок\s*$/;
const REPORT_LINE = /\n?Подробный отчёт: \S*\/reports\/(\d{4}-\d{2})\S*\n?/;

export interface FeedItem {
  text: string;
  /** куда ведёт кнопка под уведомлением (в приложении, а не ссылка из письма) */
  link: { to: string; label: string } | null;
  /** уведомление тренеру о клиенте: перед переходом по ссылке открываем дневник этого клиента */
  client: { id: string; displayName: string } | null;
}

/**
 * Уведомление пишется как письмо. В ленте приложения подпись "— Vegas" лишняя,
 * а строку "Подробный отчёт: http://..." заменяем кнопкой, ведущей на страницу отчёта внутри приложения.
 */
export function toFeedItem(notification: AppNotification): FeedItem {
  let text = notification.body.replace(SIGNATURE, '');
  let link: FeedItem['link'] = null;
  if (notification.clientId) {
    const displayName = notification.clientName ?? 'Клиент';
    // о клиенте — ведём в его дневник: главная клиента, там те же карточки рекордов, плато и перегрузки
    return {
      text: text.trim(),
      link: { to: '/', label: `Открыть дневник: ${displayName}` },
      client: { id: notification.clientId, displayName },
    };
  }
  const report = text.match(REPORT_LINE);
  if (report) {
    text = text.replace(REPORT_LINE, '\n');
    link = { to: `/reports/${report[1]}`, label: 'Открыть отчёт' };
  } else if (notification.type === 'OVERLOAD_RISK' || notification.type === 'PLATEAU') {
    link = { to: '/exercises', label: 'К упражнениям' };
  }
  // после вырезанных строк могли остаться лишние пустые строки — оставляем не больше одной подряд
  return { text: text.replace(/\n{3,}/g, '\n\n').trim(), link, client: null };
}

/** "99+" вместо трёхзначных чисел на колокольчике */
export function badgeText(count: number): string {
  return count > 99 ? '99+' : String(count);
}
