import { describe, expect, it } from 'vitest';
import type { PersonalRecord } from '../api/analytics';
import type { Workout } from '../api/workouts';
import { groupByMonth, recordsByWorkout } from './history';

const workout = (id: string, startedAt: string): Workout => ({
  id,
  name: 'Верх',
  templateId: null,
  status: 'COMPLETED',
  startedAt,
  completedAt: startedAt,
  durationSec: 3600,
  workingSets: 10,
  tonnageKg: 5000,
  exercises: [],
});

describe('groupByMonth', () => {
  it('подряд идущие тренировки одного месяца — в одной группе', () => {
    const groups = groupByMonth([
      workout('a', '2026-10-20T15:00:00'),
      workout('b', '2026-10-03T15:00:00'),
      workout('c', '2026-09-28T15:00:00'),
    ]);

    expect(groups.map((g) => g.title)).toEqual(['Октябрь 2026', 'Сентябрь 2026']);
    expect(groups[0].workouts.map((w) => w.id)).toEqual(['a', 'b']);
  });
});

describe('recordsByWorkout', () => {
  it('рекорды раскладываются по тренировкам', () => {
    const record = (workoutId: string, exerciseId: string) =>
      ({
        workoutId,
        exerciseId,
        exerciseName: '',
        achievedAt: '',
        weightKg: null,
        reps: null,
        records: [],
      }) as PersonalRecord;

    const map = recordsByWorkout([record('w1', 'bench'), record('w1', 'squat'), record('w2', 'bench')]);

    expect([...map.get('w1')!]).toEqual(['bench', 'squat']);
    expect(map.has('w3')).toBe(false);
  });
});
