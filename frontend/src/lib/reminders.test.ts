import { describe, expect, it } from 'vitest';
import type { WeeklyPlan, WorkoutTemplate } from '../api/workouts';
import { planDays, sameDays, toTimeInput, toggleDay } from './reminders';

const template = { id: 't1', name: 'Верх' } as WorkoutTemplate;

describe('reminders', () => {
  it('дни плана — только те, где стоит комплекс', () => {
    const plan: WeeklyPlan = {
      days: [
        { dayOfWeek: 'MONDAY', template },
        { dayOfWeek: 'TUESDAY', template: null },
        { dayOfWeek: 'THURSDAY', template },
      ],
    };
    expect(planDays(plan)).toEqual(['MONDAY', 'THURSDAY']);
    expect(planDays(undefined)).toEqual([]);
  });

  it('переключение дня сохраняет порядок недели', () => {
    expect(toggleDay(['FRIDAY'], 'MONDAY')).toEqual(['MONDAY', 'FRIDAY']);
    expect(toggleDay(['MONDAY', 'FRIDAY'], 'MONDAY')).toEqual(['FRIDAY']);
  });

  it('сравнение дней без учёта порядка', () => {
    expect(sameDays(['MONDAY', 'FRIDAY'], ['FRIDAY', 'MONDAY'])).toBe(true);
    expect(sameDays(['MONDAY'], ['MONDAY', 'FRIDAY'])).toBe(false);
  });

  it('время для поля ввода', () => {
    expect(toTimeInput('19:00:00')).toBe('19:00');
    expect(toTimeInput(null)).toBe('19:00');
  });
});
