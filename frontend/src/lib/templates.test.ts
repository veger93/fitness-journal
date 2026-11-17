import { describe, expect, it } from 'vitest';
import { canUpdateTemplate, findSameTemplate, move, templateChanges } from './templates';

describe('templates', () => {
  it('изменения тренировки против комплекса: добавленные и убранные, порядок не важен', () => {
    expect(templateChanges(['a', 'b', 'c'], ['c', 'a', 'd'])).toEqual({ added: ['d'], removed: ['b'] });
    expect(templateChanges(['a', 'b'], ['b', 'a'])).toEqual({ added: [], removed: [] });
  });

  it('обновить предлагаем только своему комплексу и не дню программы', () => {
    expect(canUpdateTemplate({ custom: true, program: false })).toBe(true);
    expect(canUpdateTemplate({ custom: true, program: true })).toBe(false);
    expect(canUpdateTemplate({ custom: false, program: false })).toBe(false);
    expect(canUpdateTemplate(undefined)).toBe(false);
  });

  it('перестановка вверх/вниз, за край — без изменений', () => {
    expect(move(['a', 'b', 'c'], 1, -1)).toEqual(['b', 'a', 'c']);
    expect(move(['a', 'b', 'c'], 1, 1)).toEqual(['a', 'c', 'b']);
    expect(move(['a', 'b'], 0, -1)).toEqual(['a', 'b']);
  });

  it('такой же свой комплекс: тот же набор упражнений в любом порядке, готовые и дни программы не считаются', () => {
    const tpl = (ids: string[], custom = true, program = false) => ({
      custom,
      program,
      exercises: ids.map((id) => ({ id, name: id })),
    });
    const mine = tpl(['a', 'b']);
    expect(findSameTemplate([tpl(['a']), mine], ['b', 'a'])).toBe(mine);
    expect(findSameTemplate([mine], ['a', 'b', 'c'])).toBeUndefined();
    expect(findSameTemplate([tpl(['a', 'b'], false)], ['a', 'b'])).toBeUndefined();
    expect(findSameTemplate([tpl(['a', 'b'], true, true)], ['a', 'b'])).toBeUndefined();
    expect(findSameTemplate([mine], [])).toBeUndefined();
    expect(findSameTemplate(undefined, ['a'])).toBeUndefined();
  });
});
