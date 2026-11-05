import { describe, expect, it } from 'vitest';
import { fitTitle, headline } from './shareImage';

// "ширина" текста: 0,6 размера шрифта на символ — грубо, но для проверки логики переносов хватает
const measure = (text: string, size: number) => text.length * size * 0.6;

describe('fitTitle', () => {
  it('короткое название — одна строка крупным шрифтом', () => {
    expect(fitTitle('Жим лёжа', 904, measure)).toEqual({ size: 76, lines: ['Жим лёжа'] });
  });

  it('длинное — две строки, при необходимости шрифт меньше', () => {
    const title = fitTitle('Горизонтальная тяга в рычажном тренажёре', 904, measure);
    expect(title.lines).toHaveLength(2);
    expect(title.lines.join(' ')).toBe('Горизонтальная тяга в рычажном тренажёре');
  });

  it('совсем длинное — две строки и многоточие, ничего не вылезает', () => {
    const title = fitTitle('Сгибания с гантелями на наклонной скамье обратным хватом стоя у стены', 600, measure);
    expect(title.lines).toHaveLength(2);
    expect(title.lines[1].endsWith('…')).toBe(true);
    title.lines.forEach((line) => expect(measure(line, title.size)).toBeLessThanOrEqual(600));
  });
});

describe('headline', () => {
  it('с весом — сила и рост; без веса — только рост', () => {
    expect(headline({ strengthKg: 102, changePercent: 18, showWeight: true })).toMatchObject({
      value: '102 кг',
      growth: '+18%',
    });
    expect(headline({ strengthKg: 102, changePercent: 18, showWeight: false })).toMatchObject({
      value: '+18%',
      growth: null,
    });
  });
});
