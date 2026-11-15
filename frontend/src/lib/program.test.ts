import { describe, expect, it } from 'vitest';
import type { ProgramDay } from '../api/programs';
import {
  dayPreview,
  formatRest,
  minutesHint,
  programReasons,
  programSummary,
  programWeek,
  restDaysText,
  slotScheme,
  weekDaysText,
} from './program';

const day = (name: string, weekDays: ProgramDay['weekDays'], exercises: string[]): ProgramDay => ({
  templateId: name,
  name,
  weekDays,
  slots: exercises.map((exerciseName, position) => ({
    id: `${name}-${position}`,
    position,
    exerciseId: exerciseName,
    exerciseName,
    equipment: 'BARBELL',
    imageKey: null,
    movementPattern: 'SQUAT',
    sets: 3,
    repsMin: 8,
    repsMax: 12,
    restSec: 120,
    swapped: false,
    originalExerciseName: null,
  })),
});

describe('program — тексты анкеты и программы', () => {
  it('строка-сводка', () => {
    expect(programSummary({ goal: 'MUSCLE', daysPerWeek: 3, sessionMinutes: 60, location: 'GYM' })).toBe(
      'Набрать мышцы · 3 дня · ~60 мин · зал',
    );
    expect(programSummary({ goal: 'FAT_LOSS', daysPerWeek: 5, sessionMinutes: 75, location: 'HOME_DUMBBELLS' })).toBe(
      'Похудеть и подтянуться · 5 дней · 75+ мин · дома',
    );
  });

  it('время → упражнений, как в генераторе (45:5, 60:6, 75:7)', () => {
    expect(minutesHint(45)).toBe('≈ 5 упражнений за тренировку.');
    expect(minutesHint(60)).toBe('≈ 6 упражнений за тренировку.');
    expect(minutesHint(75)).toBe('≈ 7 упражнений за тренировку.');
  });

  it('схема подходов и отдых', () => {
    expect(formatRest(120)).toBe('2 мин');
    expect(formatRest(90)).toBe('1,5 мин');
    expect(formatRest(45)).toBe('45 с');
    expect(slotScheme({ sets: 3, repsMin: 4, repsMax: 6, restSec: 180 })).toBe('3 × 4–6 · отдых 3 мин');
    expect(slotScheme({ sets: 2, repsMin: 10, repsMax: 10, restSec: 90 })).toBe('2 × 10 · отдых 1,5 мин');
  });

  it('дни недели — по порядку недели, а не как пришли', () => {
    expect(weekDaysText(['FRIDAY', 'MONDAY'])).toBe('Пн · Пт');
    expect(weekDaysText([])).toBe('');
    const days = [day('A', ['MONDAY', 'FRIDAY'], []), day('B', ['WEDNESDAY'], [])];
    expect(restDaysText(days)).toBe('Вт, Чт, Сб, Вс');
  });

  it('первые упражнения дня', () => {
    const d = day('A', ['MONDAY'], ['Присед', 'Жим лёжа', 'Тяга', 'Жим стоя', 'Бицепс']);
    expect(dayPreview(d)).toBe('присед, жим лёжа, тяга +2');
    expect(dayPreview(day('B', [], ['Присед']))).toBe('присед');
  });

  it('неделя программы: с 1-й по 6-ю', () => {
    const start = '2026-11-02T10:00:00Z';
    expect(programWeek(start, new Date('2026-11-02T12:00:00Z'))).toBe(1);
    expect(programWeek(start, new Date('2026-11-09T12:00:00Z'))).toBe(2);
    expect(programWeek(start, new Date('2027-01-30T12:00:00Z'))).toBe(6);
  });

  it('«почему так» — по цели и числу дней', () => {
    expect(programReasons({ goal: 'MUSCLE', daysPerWeek: 3 })[0].title).toBe('Каждая мышца — 3 раза в неделю');
    expect(programReasons({ goal: 'MUSCLE', daysPerWeek: 4 })[0].title).toBe('Каждая мышца — 2 раза в неделю');
    expect(programReasons({ goal: 'FAT_LOSS', daysPerWeek: 2 })[1].text).toContain('150–300 минут');
    expect(programReasons({ goal: 'STRENGTH', daysPerWeek: 5 })[1].title).toBe('В базовых — 4–6 повторов');
  });
});
