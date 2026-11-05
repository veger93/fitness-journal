import { describe, expect, it } from 'vitest';
import type { PersonalRecord } from '../api/analytics';
import { bestPerExercise, groupByMonth, recordSummary } from './RecordsPage';

const record = (exerciseId: string, achievedAt: string, weightKg: number): PersonalRecord => ({
  workoutId: `w-${achievedAt}`,
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

describe('строка рекорда', () => {
  const base = record('bench', '2026-11-04T10:00:00Z', 92.5);

  it('новый вес: ★ и «было»', () => {
    const summary = recordSummary({
      ...base,
      records: [
        { type: 'WEIGHT', valueKg: 92.5, deltaKg: 2.5, hint: null },
        { type: 'E1RM', valueKg: 107.9, deltaKg: 2.9, hint: null },
      ],
    });
    expect(summary.mark).toBe('★');
    expect(summary.text).toContain('было 90 кг');
  });

  it('только сила: ↗ и пояснение с сервера', () => {
    const summary = recordSummary({
      ...base,
      records: [{ type: 'E1RM', valueKg: 110, deltaKg: 2, hint: 'тот же вес, но на 2 повтора больше' }],
    });
    expect(summary.mark).toBe('↗');
    expect(summary.text).toContain('тот же вес, но на 2 повтора больше');
  });
});
