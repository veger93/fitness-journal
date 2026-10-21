import { describe, expect, it } from 'vitest';
import type { PersonalRecord } from '../api/analytics';
import { bestPerExercise, groupByMonth } from './RecordsPage';

const record = (exerciseId: string, achievedAt: string, weightKg: number): PersonalRecord => ({
  exerciseId,
  exerciseName: exerciseId,
  achievedAt,
  weightKg,
  reps: 5,
  records: [],
});

describe('рекорды', () => {
  // как приходят с сервера: новые сверху
  const records = [
    record('bench', '2026-10-18T10:00:00Z', 100),
    record('squat', '2026-10-02T10:00:00Z', 140),
    record('bench', '2026-09-20T10:00:00Z', 95),
  ];

  it('лучшие — самый свежий рекорд каждого упражнения', () => {
    expect(bestPerExercise(records).map((r) => [r.exerciseId, r.weightKg])).toEqual([
      ['bench', 100],
      ['squat', 140],
    ]);
  });

  it('история — по месяцам, новые сверху', () => {
    expect(groupByMonth(records).map((g) => [g.month, g.items.length])).toEqual([
      ['октябрь 2026', 2],
      ['сентябрь 2026', 1],
    ]);
  });
});
