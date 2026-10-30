import { describe, expect, it } from 'vitest';
import type { Workout, WorkoutSet } from '../api/workouts';
import {
  describeSet,
  distanceFromSpeed,
  formatClock,
  formatDuration,
  formatPace,
  formatSpeed,
  runParts,
  suggestNextSet,
  summarizeSets,
  toRequest,
  withSetAdded,
  withSetDeleted,
} from './workout';

const set = (number: number, weightKg: number, reps: number): WorkoutSet => ({
  id: `s${number}`,
  number,
  weightKg,
  reps,
  durationSec: null,
  distanceM: null,
  rpe: null,
  warmup: false,
});

describe('suggestNextSet', () => {
  it('разгрузка важнее прошлых подходов: облегчённый вес и повторы из плана', () => {
    const deload = { exerciseId: 'e', workingWeightKg: 70, sets: 3, reps: 5, startedAt: '', endsAt: '' };

    expect(suggestNextSet([set(1, 100, 5)], [set(1, 100, 5)], 'WEIGHT_REPS', 'BARBELL', deload)).toMatchObject({
      weightKg: 70,
      reps: 5,
    });
  });

  it('повторяет последний подход сегодня', () => {
    expect(suggestNextSet([set(1, 80, 8), set(2, 82.5, 6)], [set(1, 70, 10)])).toMatchObject({
      weightKg: 82.5,
      reps: 6,
    });
  });

  it('первый подход — самый тяжёлый рабочий подход прошлого раза (а не первый по счёту)', () => {
    // в прошлый раз: 20 × 10, 30 × 10, 40 × 10, 40 × 8 — сегодня начинаем с 40 (повторы — последнего такого)
    expect(suggestNextSet([], [set(1, 20, 10), set(2, 30, 10), set(3, 40, 10), set(4, 40, 8)])).toMatchObject({
      weightKg: 40,
      reps: 8,
    });
  });

  it('разминку прошлого раза не считаем, даже если она тяжелее по ошибке', () => {
    const warmup = { ...set(1, 60, 5), warmup: true };
    expect(suggestNextSet([], [warmup, set(2, 50, 8)])).toMatchObject({ weightKg: 50, reps: 8 });
  });

  it('бег впервые — 5 км за 30 минут', () => {
    expect(suggestNextSet([], [], 'DISTANCE')).toMatchObject({ distanceM: 5000, durationSec: 1800 });
  });

  it('впервые: стартовый вес по снаряжению', () => {
    expect(suggestNextSet([], [], 'WEIGHT_REPS', 'BARBELL').weightKg).toBe(20);
    expect(suggestNextSet([], [], 'WEIGHT_REPS', 'DUMBBELL').weightKg).toBe(6);
    expect(suggestNextSet([], [], 'WEIGHT_REPS', 'BODYWEIGHT').weightKg).toBe(0); // подтягивания — без "20 кг"
  });

  it('упражнение впервые — значения по умолчанию', () => {
    expect(suggestNextSet([], [])).toMatchObject({ weightKg: 20, reps: 10, warmup: false });
  });
});

describe('toRequest', () => {
  it('отправляет только поля своего вида упражнения', () => {
    const input = { weightKg: 80, reps: 8, durationSec: 60, distanceM: 1000, rpe: 8, warmup: false };
    expect(toRequest('WEIGHT_REPS', input)).toEqual({
      weightKg: 80,
      reps: 8,
      durationSec: null,
      distanceM: null,
      rpe: 8,
      warmup: false,
    });
    expect(toRequest('TIME', input)).toMatchObject({ weightKg: null, reps: null, durationSec: 60 });
    expect(toRequest('DISTANCE', input)).toMatchObject({ weightKg: null, distanceM: 1000, durationSec: 60 });
  });
});

describe('форматирование', () => {
  it('подход', () => {
    expect(describeSet('WEIGHT_REPS', { weightKg: 82.5, reps: 6, durationSec: null, distanceM: null })).toBe(
      '82,5 кг × 6',
    );
    expect(describeSet('WEIGHT_REPS', { weightKg: 0, reps: 15, durationSec: null, distanceM: null })).toBe('15 повт.');
    expect(describeSet('TIME', { weightKg: null, reps: null, durationSec: 90, distanceM: null })).toBe('1:30');
    expect(describeSet('DISTANCE', { weightKg: null, reps: null, durationSec: null, distanceM: 1500 })).toBe('1,5 км');
    expect(describeSet('DISTANCE', { weightKg: null, reps: null, durationSec: 1600, distanceM: 5000 })).toBe(
      '5 км · 26:40 · 11,3 км/ч',
    );
    expect(describeSet('DISTANCE', { weightKg: null, reps: null, durationSec: 90, distanceM: 400 })).toBe(
      '400 м · 1:30 · 16 км/ч',
    );
  });

  it('таймер и длительность', () => {
    expect(formatClock(1451)).toBe('24:11');
    expect(formatClock(3725)).toBe('1:02:05');
    expect(formatDuration(42 * 60)).toBe('42 мин');
    expect(formatDuration(70 * 60)).toBe('1 ч 10 мин');
  });
});

describe('бег', () => {
  it('скорость и темп — одно и то же, разными словами', () => {
    expect(formatSpeed(37 * 60 + 30, 5000)).toBe('8 км/ч');
    expect(formatPace(37 * 60 + 30, 5000)).toBe('7:30 /км');
  });

  it('дорожка: 8 км/ч × 37:30 = 5 км', () => {
    expect(distanceFromSpeed(8, 37 * 60 + 30)).toBe(5000);
  });

  it('строка отрезка — в две строки', () => {
    expect(runParts(5000, 2250)).toEqual({ main: '5 км · 37:30', sub: '8 км/ч · 7:30 /км' });
  });
});

describe('summarizeSets', () => {
  it('одинаковый вес подряд — один раз', () => {
    const warm = { ...set(0, 40, 10), warmup: true };
    expect(summarizeSets('WEIGHT_REPS', [warm, set(1, 80, 8), set(2, 80, 8), set(3, 80, 6), set(4, 85, 5)])).toBe(
      '80 кг × 8, 8, 6 · 85 кг × 5',
    );
  });

  it('без веса — только повторы', () => {
    expect(
      summarizeSets('WEIGHT_REPS', [
        { ...set(1, 0, 12), weightKg: null },
        { ...set(2, 0, 10), weightKg: null },
      ]),
    ).toBe('12, 10 повт.');
  });
});

describe('обновление кэша', () => {
  const workout: Workout = {
    id: 'w',
    name: 'Грудь',
    templateId: null,
    status: 'IN_PROGRESS',
    startedAt: '',
    completedAt: null,
    durationSec: null,
    workingSets: 0,
    tonnageKg: 0,
    exercises: [
      {
        id: 'e1',
        exerciseId: 'x',
        exerciseName: 'Жим',
        trackingType: 'WEIGHT_REPS',
        equipment: 'BARBELL',
        sets: [set(1, 80, 8), set(2, 80, 8)],
        previousSets: [],
        deload: null,
      },
    ],
  };

  it('добавление не меняет исходный объект', () => {
    const updated = withSetAdded(workout, 'e1', set(3, 80, 7));
    expect(updated.exercises[0].sets).toHaveLength(3);
    expect(workout.exercises[0].sets).toHaveLength(2);
  });

  it('после удаления номера идут подряд', () => {
    const updated = withSetDeleted(withSetAdded(workout, 'e1', set(3, 80, 7)), 'e1', 's2');
    expect(updated.exercises[0].sets.map((s) => [s.id, s.number])).toEqual([
      ['s1', 1],
      ['s3', 2],
    ]);
  });
});
