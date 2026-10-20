import { describe, expect, it } from 'vitest';
import { formatChange, formatKg, formatMonth, formatSet, formatTonnage } from './format';

// Intl в Node ставит неразрывный пробел между разрядами — нормализуем для читаемости тестов
const plain = (s: string | null) => s?.replace(/ | /g, ' ');

describe('format', () => {
  it('килограммы и тоннаж', () => {
    expect(plain(formatKg(102.5))).toBe('102,5 кг');
    expect(plain(formatTonnage(850))).toBe('850 кг');
    expect(plain(formatTonnage(14230))).toBe('14,2 т');
  });

  it('изменение со знаком', () => {
    expect(formatChange(8)).toBe('+8%');
    expect(formatChange(-2.5)).toBe('−2,5%');
    expect(formatChange(0)).toBe('0%');
  });

  it('подход', () => {
    expect(plain(formatSet(80, 5))).toBe('80 кг × 5');
    expect(plain(formatSet(null, 20))).toBe('20 повт.');
    expect(formatSet(null, null)).toBeNull();
  });

  it('месяц', () => {
    expect(formatMonth('2026-10')).toBe('октябрь');
  });
});
