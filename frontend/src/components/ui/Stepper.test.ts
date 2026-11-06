import { describe, expect, it } from 'vitest';
import { formatStepperValue, parseStepperInput } from './Stepper';

describe('поле с числом (Stepper)', () => {
  it('показывает дробь через запятую', () => {
    expect(formatStepperValue(79.2)).toBe('79,2');
    expect(formatStepperValue(80)).toBe('80');
    expect(formatStepperValue(Number.NaN)).toBe('');
  });

  it('принимает и запятую, и точку', () => {
    expect(parseStepperInput('79,2', 2)).toBe(79.2);
    expect(parseStepperInput('79.2', 2)).toBe(79.2);
    expect(parseStepperInput('79,', 2)).toBe(79); // ещё печатает
  });

  it('пустое поле — NaN, мусор — не принимаем', () => {
    expect(parseStepperInput('', 2)).toBeNaN();
    expect(parseStepperInput('79,2,1', 2)).toBeNull();
    expect(parseStepperInput('abc', 2)).toBeNull();
  });

  it('целые: запятую не принимаем', () => {
    expect(parseStepperInput('12', 0)).toBe(12);
    expect(parseStepperInput('12,5', 0)).toBeNull();
  });
});
