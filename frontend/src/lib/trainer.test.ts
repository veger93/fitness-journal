import { describe, expect, it } from 'vitest';
import { formatDuration, initial, minutesSince, signalBadge, workoutStatusText } from './trainer';

describe('trainer utils', () => {
  it('initial — первая буква имени заглавной', () => {
    expect(initial(' иван петров')).toBe('И');
    expect(initial('')).toBe('?');
    expect(initial(null)).toBe('?');
  });

  it('minutesSince — целые минуты, будущее не уходит в минус', () => {
    const now = new Date('2026-10-30T10:38:30Z');
    expect(minutesSince('2026-10-30T10:00:00Z', now)).toBe(38);
    expect(minutesSince('2026-10-30T11:00:00Z', now)).toBe(0);
  });

  it('formatDuration', () => {
    expect(formatDuration(38)).toBe('38 мин');
    expect(formatDuration(65)).toBe('1 ч 05 мин');
  });

  it('workoutStatusText — идёт или только подготовлена', () => {
    const now = new Date('2026-10-30T10:38:00Z');
    expect(workoutStatusText('2026-10-30T10:00:00Z', now)).toBe('идёт · 38 мин');
    expect(workoutStatusText(null, now)).toBe('не начата');
  });

  it('signalBadge — текст и вид метки клиента', () => {
    const base = { clientId: 'c', exerciseName: 'Жим', lastWorkoutAt: null };
    expect(signalBadge({ ...base, signal: 'OVERLOAD', days: null })).toEqual({ text: 'перегрузка', variant: 'accent' });
    expect(signalBadge({ ...base, signal: 'PLATEAU', days: 21 })?.text).toBe('плато · 3 нед');
    expect(signalBadge({ ...base, signal: 'AWAY', days: 9 })?.text).toBe('9 дн без тренировок');
    expect(signalBadge({ ...base, signal: 'RECORD', days: 1 })?.text).toBe('рекорд · вчера');
    expect(signalBadge({ ...base, signal: 'NONE', days: null })).toBeNull();
    expect(signalBadge(undefined)).toBeNull();
  });
});
