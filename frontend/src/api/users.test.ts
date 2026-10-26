import { describe, expect, it } from 'vitest';
import { localDate } from './users';

describe('localDate', () => {
  it('дата по местным часам, с ведущими нулями', () => {
    expect(localDate(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05');
  });

  it('поздний вечер остаётся сегодняшним днём (toISOString сдвинул бы в UTC)', () => {
    const lateEvening = new Date(2026, 9, 27, 23, 30);
    expect(localDate(lateEvening)).toBe('2026-10-27');
  });
});
