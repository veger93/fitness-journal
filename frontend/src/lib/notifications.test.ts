import { describe, expect, it } from 'vitest';
import type { AppNotification } from '../api/notifications';
import { badgeText, toFeedItem } from './notifications';

const notification = (type: AppNotification['type'], body: string): AppNotification => ({
  id: '1',
  type,
  subject: 's',
  body,
  status: 'SENT',
  createdAt: '2026-10-27T10:00:00Z',
  read: false,
});

describe('toFeedItem', () => {
  it('убирает подпись письма', () => {
    const item = toFeedItem(
      notification('PERSONAL_RECORD', 'Сергей, новый рекорд!\n\nЖим 100 кг\n\n—\nVegas, дневник тренировок'),
    );

    expect(item.text).toBe('Сергей, новый рекорд!\n\nЖим 100 кг');
    expect(item.link).toBeNull();
  });

  it('ссылку на отчёт из письма превращает в кнопку внутри приложения', () => {
    const body =
      'Сергей, итоги за октябрь 2026:\n\n• Тренировок: 11\n\nПодробный отчёт: http://localhost:5173/reports/2026-10\n\n—\nVegas, дневник тренировок';

    const item = toFeedItem(notification('MONTHLY_REPORT', body));

    expect(item.link).toEqual({ to: '/reports/2026-10', label: 'Открыть отчёт' });
    expect(item.text).not.toContain('http');
    expect(item.text).toContain('• Тренировок: 11');
    expect(item.text).not.toMatch(/\n{3,}/);
  });

  it('перегрузка и плато ведут к упражнениям', () => {
    expect(toFeedItem(notification('OVERLOAD_RISK', 'текст')).link?.to).toBe('/exercises');
  });
});

describe('badgeText', () => {
  it('большие числа сокращает', () => {
    expect(badgeText(7)).toBe('7');
    expect(badgeText(150)).toBe('99+');
  });
});
