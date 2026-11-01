import { describe, expect, it } from 'vitest';
import { demoTimeLeft } from './demo';

describe('demoTimeLeft', () => {
  it('часы, когда осталось больше часа; иначе минуты; не уходит в минус', () => {
    const now = new Date('2026-11-01T10:00:00Z');
    expect(demoTimeLeft('2026-11-02T09:30:00Z', now)).toBe('23 ч');
    expect(demoTimeLeft('2026-11-01T10:40:00Z', now)).toBe('40 мин');
    expect(demoTimeLeft('2026-11-01T09:00:00Z', now)).toBe('0 мин');
  });
});
