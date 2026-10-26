import { describe, expect, it } from 'vitest';
import type { BodyPart, WeeklyPlan, WorkoutTemplate } from '../api/workouts';
import type { Workout } from '../api/workouts';
import { bodyPartFrequency, completedToday, nextPlanned, planHints, todayOf } from './plan';

const template = (name: string, parts: BodyPart[]): WorkoutTemplate => ({
  id: name,
  name,
  color: 'blue',
  icon: null,
  custom: true,
  exercises: parts.map((bodyPart, i) => ({
    id: `${name}${i}`,
    name: `${name}${i}`,
    bodyPart,
    trackingType: 'WEIGHT_REPS',
  })),
});

const upper = template('Верх', ['CHEST', 'BACK', 'CHEST']);
const lower = template('Низ', ['LEGS', 'CORE']);

const plan = (byDay: Partial<Record<string, WorkoutTemplate>>): WeeklyPlan => ({
  days: (['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const).map((d) => ({
    dayOfWeek: d,
    template: byDay[d] ?? null,
  })),
});

describe('план на неделю', () => {
  it('сегодня: неделя с понедельника', () => {
    expect(todayOf(new Date('2026-10-26T12:00:00'))).toBe('MONDAY');
    expect(todayOf(new Date('2026-10-25T12:00:00'))).toBe('SUNDAY');
  });

  it('следующая тренировка — по кругу недели', () => {
    const p = plan({ MONDAY: upper, THURSDAY: lower });
    expect(nextPlanned(p, 'MONDAY')).toMatchObject({ day: 'THURSDAY', template: { name: 'Низ' } });
    expect(nextPlanned(p, 'FRIDAY')).toMatchObject({ day: 'MONDAY', template: { name: 'Верх' } });
    expect(nextPlanned(plan({}), 'MONDAY')).toBeNull();
  });

  it('частота группы — раз в день, сколько бы упражнений ни было', () => {
    const counts = bodyPartFrequency([upper, null, lower, null, upper]);
    expect(counts.get('CHEST')).toBe(2);
    expect(counts.get('LEGS')).toBe(1);
  });

  it('подсказки: группа раз в неделю (кроме пресса) и неделя без отдыха', () => {
    expect(planHints([upper, null, lower, null, upper, null, null])).toEqual([
      expect.stringContaining('Раз в неделю: ноги'),
    ]);
    expect(planHints([upper, lower, upper, lower, upper, lower, upper])[0]).toContain('Без дня отдыха');
    expect(planHints([upper, lower, upper, lower, null, null, null])).toEqual([]);
  });

  it('сегодняшняя тренировка по плану выполнена — только тот же комплекс и сегодня', () => {
    const now = new Date('2026-10-26T20:00:00');
    const done = (templateId: string, completedAt: string) =>
      ({ id: completedAt, templateId, completedAt }) as unknown as Workout;
    expect(completedToday([done('t-legs', '2026-10-26T18:30:00')], 't-legs', now)).not.toBeNull();
    expect(completedToday([done('t-upper', '2026-10-26T18:30:00')], 't-legs', now)).toBeNull();
    expect(completedToday([done('t-legs', '2026-10-25T18:30:00')], 't-legs', now)).toBeNull();
  });
});
