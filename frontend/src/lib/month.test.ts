import { describe, expect, it } from 'vitest';
import { addMonths, currentMonth, monthTitle, summaryMonthToShow } from './month';

describe('month', () => {
  it('текущий месяц по местным часам', () => {
    expect(currentMonth(new Date(2026, 9, 31, 23, 59))).toBe('2026-10');
  });

  it('переход через год в обе стороны', () => {
    expect(addMonths('2026-01', -1)).toBe('2025-12');
    expect(addMonths('2025-12', 1)).toBe('2026-01');
    expect(addMonths('2026-10', -13)).toBe('2025-09');
  });

  it('название месяца', () => {
    expect(monthTitle('2026-10')).toBe('октябрь 2026');
  });

  it('итоги прошлого месяца — только первую неделю', () => {
    expect(summaryMonthToShow(new Date(2026, 10, 3))).toBe('2026-10');
    expect(summaryMonthToShow(new Date(2027, 0, 1))).toBe('2026-12');
    expect(summaryMonthToShow(new Date(2026, 10, 8))).toBeNull();
  });
});
