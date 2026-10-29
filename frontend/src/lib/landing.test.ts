import { describe, expect, it } from 'vitest';
import { chartPaths } from './landing';

describe('chartPaths', () => {
  const paths = chartPaths([80, 85, 90], [80, 84, 88], 200, 100);

  it('линия идёт слева направо через все точки', () => {
    expect(paths.line.startsWith('M6.0 ')).toBe(true);
    expect(paths.line.split('L')).toHaveLength(3);
  });

  it('рост силы — линия поднимается (в SVG вверх = меньше y)', () => {
    const ys = paths.line
      .split(/[ML]/)
      .filter(Boolean)
      .map((p) => Number(p.trim().split(' ')[1]));
    expect(ys[2]).toBeLessThan(ys[0]);
  });

  it('последняя точка — в правом конце линии', () => {
    expect(paths.last.x).toBeCloseTo(194);
    expect(paths.line.endsWith(`${paths.last.x.toFixed(1)} ${paths.last.y.toFixed(1)}`)).toBe(true);
  });

  it('заливка замкнута по низу графика', () => {
    expect(paths.area.endsWith('Z')).toBe(true);
    expect(paths.area).toContain('L194.0 100');
  });
});
