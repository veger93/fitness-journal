import { describe, expect, it } from 'vitest';
import type { ExerciseSummary } from '../api/analytics';
import type { BodyPart, CatalogExercise, Equipment } from '../api/workouts';
import { countByBodyPart, equipmentIn, exercisesCount, filterCatalog, recentExercises } from './catalog';

const ex = (id: string, name: string, bodyPart: BodyPart, equipment: Equipment): CatalogExercise => ({
  id,
  name,
  bodyPart,
  equipment,
  trackingType: 'WEIGHT_REPS',
  custom: false,
  muscles: [],
  imageKey: null,
});

const catalog = [
  ex('1', 'Жим лёжа', 'CHEST', 'BARBELL'),
  ex('2', 'Жим гантелей лёжа', 'CHEST', 'DUMBBELL'),
  ex('3', 'Отжимания от пола', 'CHEST', 'BODYWEIGHT'),
  ex('4', 'Подтягивания', 'BACK', 'BODYWEIGHT'),
  ex('5', 'Жим ногами', 'LEGS', 'MACHINE'),
];

const summary = (exerciseId: string, lastPerformedAt: string) => ({ exerciseId, lastPerformedAt }) as ExerciseSummary;

describe('countByBodyPart', () => {
  it('считает упражнения в каждой части тела, пустые — ноль', () => {
    const counts = countByBodyPart(catalog);
    expect(counts.CHEST).toBe(3);
    expect(counts.BACK).toBe(1);
    expect(counts.CORE).toBe(0);
  });
});

describe('filterCatalog', () => {
  it('часть тела и снаряд вместе', () => {
    const result = filterCatalog(catalog, { search: '', bodyPart: 'CHEST', equipment: 'DUMBBELL' });
    expect(result.map((e) => e.id)).toEqual(['2']);
  });

  it('поиск идёт по всему каталогу, без учёта выбранной категории', () => {
    // регистр не важен; "Отжимания" тоже содержат "жим" — это нормально для поиска по подстроке
    const result = filterCatalog(catalog, { search: 'ЖИМ', bodyPart: 'BACK', equipment: null });
    expect(result.map((e) => e.id)).toEqual(['1', '2', '3', '5']);
  });

  it('только свои упражнения', () => {
    const withCustom = [...catalog, { ...ex('6', 'Мой жим', 'CHEST', 'BARBELL'), custom: true }];
    const result = filterCatalog(withCustom, { search: '', bodyPart: null, equipment: null, customOnly: true });
    expect(result.map((e) => e.id)).toEqual(['6']);
  });

  it('без фильтров — весь каталог', () => {
    expect(filterCatalog(catalog, { search: '  ', bodyPart: null, equipment: null })).toHaveLength(5);
  });
});

describe('equipmentIn', () => {
  it('только снаряды, которые есть в списке, в постоянном порядке', () => {
    expect(equipmentIn([catalog[2], catalog[0]])).toEqual(['BARBELL', 'BODYWEIGHT']);
  });
});

describe('recentExercises', () => {
  it('свежие первыми, удалённых из каталога нет, не больше лимита', () => {
    const summaries = [
      summary('1', '2026-10-01T10:00:00Z'),
      summary('4', '2026-10-20T10:00:00Z'),
      summary('deleted', '2026-10-25T10:00:00Z'),
      summary('5', '2026-10-10T10:00:00Z'),
    ];
    expect(recentExercises(summaries, catalog, 2).map((e) => e.id)).toEqual(['4', '5']);
  });
});

describe('exercisesCount', () => {
  it('склоняет слово', () => {
    expect(exercisesCount(1)).toBe('1 упражнение');
    expect(exercisesCount(3)).toBe('3 упражнения');
    expect(exercisesCount(11)).toBe('11 упражнений');
    expect(exercisesCount(22)).toBe('22 упражнения');
  });
});
