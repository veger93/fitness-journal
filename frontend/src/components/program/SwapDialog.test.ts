import { describe, expect, it } from 'vitest';
import type { ProgramAlternative } from '../../api/programs';
import { alternativeText } from './SwapDialog';

const alt = (patch: Partial<ProgramAlternative>): ProgramAlternative => ({
  exerciseId: '1',
  name: 'Жим гантелей лёжа',
  equipment: 'DUMBBELL',
  imageKey: null,
  familiar: false,
  lastWeightKg: null,
  lastReps: null,
  lastPerformedAt: null,
  usedInDay: null,
  ...patch,
});

const plain = (s: string) => s.replace(/ | /g, ' ');

describe('окно замены — вторая строка варианта', () => {
  it('незнакомое — коротко (совет про лёгкий вес — один раз над списком)', () => {
    expect(alternativeText(alt({}))).toBe('ещё не делали');
  });

  it('знакомое — лучший подход и дата', () => {
    expect(
      plain(
        alternativeText(
          alt({ familiar: true, lastWeightKg: 32, lastReps: 10, lastPerformedAt: '2026-10-21T10:00:00Z' }),
        ),
      ),
    ).toBe('уже делали: 32 кг × 10, 21 окт.');
  });

  it('уже есть в другом дне — предупреждаем', () => {
    expect(alternativeText(alt({ usedInDay: 'Верх B' }))).toBe('ещё не делали · уже есть в «Верх B»');
  });

  it('без веса (подтягивания) — только повторы', () => {
    expect(alternativeText(alt({ familiar: true, lastReps: 11, lastPerformedAt: null }))).toBe('уже делали: 11 повт.');
  });
});
