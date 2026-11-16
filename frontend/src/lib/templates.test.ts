import { describe, expect, it } from 'vitest';
import { canUpdateTemplate, move, templateChanges } from './templates';

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
});
