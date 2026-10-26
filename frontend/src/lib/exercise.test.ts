import { describe, expect, it } from 'vitest';
import type { ProgressPoint } from '../api/analytics';
import { bestDuration, historyRows, setsLabel } from './exercise';

const point = (date: string, extra: Partial<ProgressPoint> = {}): ProgressPoint => ({
  date,
  e1rmKg: null,
  bestWeightKg: null,
  bestReps: null,
  volumeKg: 0,
  referenceE1rmKg: null,
  workingSets: 3,
  bestDurationSec: null,
  ...extra,
});

describe('historyRows', () => {
  it('свежие тренировки сверху, исходный массив не меняется', () => {
    const points = [
      point('2026-10-01T10:00:00Z', { bestWeightKg: 80, bestReps: 5, e1rmKg: 93.3 }),
      point('2026-10-08T10:00:00Z', { bestWeightKg: 82.5, bestReps: 5, e1rmKg: 96.3, workingSets: 4 }),
    ];

    const rows = historyRows(points, 'WEIGHT_REPS');

    expect(rows.map((r) => r.date)).toEqual(['2026-10-08T10:00:00Z', '2026-10-01T10:00:00Z']);
    expect(rows[0]).toMatchObject({ best: '82,5 кг × 5', sets: '4 подхода', strength: '96,3 кг' });
    expect(points[0].date).toBe('2026-10-01T10:00:00Z');
  });

  it('упражнение на время: лучшее время, без силы', () => {
    const rows = historyRows([point('2026-10-01T10:00:00Z', { bestDurationSec: 90, workingSets: 2 })], 'TIME');

    expect(rows[0]).toMatchObject({ best: '1:30', sets: '2 подхода', strength: null });
  });

  it('без веса — только повторы', () => {
    const rows = historyRows([point('2026-10-01T10:00:00Z', { bestReps: 12 })], 'WEIGHT_REPS');

    expect(rows[0].best).toBe('12 повт.');
  });
});

describe('setsLabel', () => {
  it.each([
    [1, '1 подход'],
    [2, '2 подхода'],
    [5, '5 подходов'],
    [11, '11 подходов'],
    [12, '12 подходов'],
    [21, '21 подход'],
    [22, '22 подхода'],
  ])('%i -> %s', (count, label) => {
    expect(setsLabel(count)).toBe(label);
  });
});

describe('bestDuration', () => {
  it('максимум по периоду, без данных — null', () => {
    expect(bestDuration([point('a', { bestDurationSec: 60 }), point('b', { bestDurationSec: 95 }), point('c')])).toBe(
      95,
    );
    expect(bestDuration([point('a')])).toBeNull();
  });
});
