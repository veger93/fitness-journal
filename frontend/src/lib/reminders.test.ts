import { describe, expect, it } from 'vitest';
import type { WeeklyPlan, WorkoutTemplate } from '../api/workouts';
import { planDays, restDaysAmong, toTimeInput, toggleDay } from './reminders';

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

  it('дни отдыха среди выбранных вручную', () => {
    const plan: WeeklyPlan = { days: [{ dayOfWeek: 'MONDAY', template }] };
    expect(restDaysAmong(['MONDAY', 'TUESDAY'], plan)).toEqual(['TUESDAY']);
    // плана нет — не предупреждаем
    expect(restDaysAmong(['MONDAY', 'TUESDAY'], { days: [] })).toEqual([]);
  });

  it('время для поля ввода', () => {
    expect(toTimeInput('19:00:00')).toBe('19:00');
    expect(toTimeInput(null)).toBe('19:00');
  });
});
