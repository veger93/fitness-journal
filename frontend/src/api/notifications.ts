import { api } from './client';
import type { DayOfWeek } from './workouts';

/** Типы повторяют DTO notification-service. */
export type NotificationType =
  'PERSONAL_RECORD' | 'PLATEAU' | 'OVERLOAD_RISK' | 'MONTHLY_REPORT' | 'WORKOUT_REMINDER' | 'INACTIVITY';

export interface AppNotification {
  id: string;
  type: NotificationType;
  subject: string;
  /** текст письма как есть — для ленты убираем подпись (см. lib/notifications.ts) */
  body: string;
  status: 'SENT' | 'SKIPPED';
  createdAt: string;
  read: boolean;
}

/** PLAN — напоминать в дни плана на неделю, CUSTOM — в выбранные вручную */
export type ReminderMode = 'PLAN' | 'CUSTOM';

export interface NotificationSettings {
  remindersEnabled: boolean;
  reminderMode: ReminderMode;
  /** используется только в режиме CUSTOM */
  reminderDays: DayOfWeek[];
  /** "19:00:00" — Jackson отдаёт LocalTime с секундами, принимает и "19:00" */
  reminderTime: string | null;
  inactivityEnabled: boolean;
  inactivityDays: number;
  /** IANA, например "Europe/Moscow" */
  timeZone: string;
}

interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const base = '/api/notifications';

export const notificationsApi = {
  list: (page = 0, size = 20) => api<Page<AppNotification>>(`${base}?page=${page}&size=${size}`),
  unreadCount: () => api<{ count: number }>(`${base}/unread-count`),
  markAllRead: () => api<void>(`${base}/read`, { method: 'POST' }),
  settings: () => api<NotificationSettings>(`${base}/settings`),
  updateSettings: (body: NotificationSettings) =>
    api<NotificationSettings>(`${base}/settings`, { method: 'PUT', body }),
};
