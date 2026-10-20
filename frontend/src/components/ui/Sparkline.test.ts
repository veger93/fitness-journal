import { describe, expect, it } from 'vitest';
import { sparklinePoints } from './Sparkline';

describe('sparklinePoints', () => {
  it('рост — линия идёт вверх (y уменьшается)', () => {
    expect(sparklinePoints([100, 110, 120], 100, 30, 0)).toBe('0,30 50,15 100,0');
  });

  it('одно значение — нечего рисовать', () => {
    expect(sparklinePoints([100], 100, 30)).toBe('');
  });

  it('все равны — ровная линия посередине', () => {
    expect(sparklinePoints([90, 90], 100, 30, 0)).toBe('0,15 100,15');
  });
});
