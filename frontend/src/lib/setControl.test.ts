import { describe, expect, it } from 'vitest';
import type { ExerciseTarget, WorkoutSet } from '../api/workouts';
import { doneSets, isExerciseDone, planProgress, planText, plannedSets, weightHint } from './setControl';

const set = (weight: number, reps: number, warmup = false): WorkoutSet => ({
  id: `${weight}-${reps}-${Math.random()}`,
  number: 1,
  weightKg: weight,
  reps,
  durationSec: null,
  distanceM: null,
  rpe: null,
  warmup,
});

const target = (patch: Partial<ExerciseTarget> = {}): ExerciseTarget => ({
  sets: 3,
  repsMin: 8,
  repsMax: 12,
  restSec: 120,
  suggestion: 'KEEP',
  suggestedWeightKg: 60,
  baseWeightKg: 60,
  lastReps: [10, 10, 9],
  ...patch,
});

const plain = (s: string | null | undefined) => s?.replace(/ | /g, ' ');

describe('контроль подходов', () => {
  it('разминка не считается; цель из разгрузки важнее программы', () => {
    const e = { sets: [set(20, 10, true), set(60, 10)], deload: null, target: target() };
    expect(doneSets(e)).toBe(1);
    expect(plannedSets(e)).toBe(3);
    const deload = { exerciseId: 'x', workingWeightKg: 50, sets: 2, reps: 8, startedAt: '', endsAt: '' };
    expect(plannedSets({ ...e, deload })).toBe(2);
    expect(planText({ ...e, deload })).toBe('разгрузка: 2 × 8');
    expect(planText(e)).toBe('план: 3 × 8–12 · отдых 2 мин');
  });

  it('прогресс дня: лишние подходы не закрывают другие упражнения; без цели — не в счёт', () => {
    const progress = planProgress([
      {
        id: 'a',
        exerciseName: 'Присед',
        sets: [set(80, 8), set(80, 8), set(80, 8), set(80, 8)],
        deload: null,
        target: target(),
      },
      { id: 'b', exerciseName: 'Жим', sets: [set(60, 10)], deload: null, target: target() },
      { id: 'c', exerciseName: 'Пресс', sets: [set(0, 20)], deload: null, target: null },
    ]);
    expect(progress).toMatchObject({ done: 4, total: 6, exercisesDone: 1, exercisesTotal: 2 });
    expect(progress?.remaining).toEqual([{ id: 'b', name: 'Жим', done: 1, planned: 3 }]);
    expect(isExerciseDone({ sets: [set(60, 10)], deload: null, target: target({ sets: 1 }) })).toBe(true);
  });

  it('свободная тренировка — плана нет', () => {
    expect(planProgress([{ id: 'a', exerciseName: 'Жим', sets: [], deload: null, target: null }])).toBeNull();
  });

  it('подсказка веса: прибавить — с объяснением, прежний — с целью, новое — подобрать', () => {
    const inc = weightHint({
      sets: [],
      deload: null,
      target: target({ suggestion: 'INCREASE', suggestedWeightKg: 62.5, baseWeightKg: 60, lastReps: [12, 12, 12] }),
    });
    expect(plain(inc?.text)).toBe('+2,5 кг — в прошлые 2 раза на 60 кг было 12, 12, 12. Пора прибавить.');
    expect(plain(weightHint({ sets: [], deload: null, target: target() })?.text)).toBe(
      'В прошлый раз на 60 кг: 10, 10, 9. Цель — 12 во всех подходах, тогда прибавим.',
    );
    expect(
      weightHint({ sets: [], deload: null, target: target({ suggestion: 'NEW', baseWeightKg: null }) })?.kind,
    ).toBe('new');
  });
});
