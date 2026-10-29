/**
 * Данные и расчёты для страницы гостя (лендинга). Это ПРИМЕР демо-атлета, а не чьи-то реальные результаты.
 */

/** Жим лёжа демо-атлета: "сила дня" (одноповторный максимум) по неделям, кг */
export const DEMO_STRENGTH = [80, 81, 82.5, 82.5, 84, 85, 85, 86, 88, 90, 91, 92.5];

/** Ориентир: как в среднем растут регулярно тренирующиеся (пунктир на графике) */
export const DEMO_REFERENCE = [80, 81.5, 83, 84.3, 85.5, 86.6, 87.6, 88.5, 89.3, 90, 90.6, 91.2];

export interface ChartPaths {
  /** линия силы */
  line: string;
  /** заливка под линией */
  area: string;
  /** пунктир-ориентир */
  reference: string;
  /** последняя точка — кружок "ты сейчас здесь" */
  last: { x: number; y: number };
}

/**
 * SVG-пути графика в прямоугольнике width × height.
 * Шкала общая для линии и ориентира, с полями сверху/снизу, чтобы линии не прилипали к краям.
 */
export function chartPaths(values: number[], reference: number[], width: number, height: number): ChartPaths {
  const all = [...values, ...reference];
  const min = Math.min(...all) - 1.5;
  const max = Math.max(...all) + 1.5;
  const pad = 6;
  const x = (i: number) => (values.length < 2 ? width / 2 : pad + (i * (width - 2 * pad)) / (values.length - 1));
  const y = (v: number) => height - pad - ((v - min) / (max - min)) * (height - 2 * pad);
  const path = (series: number[]) =>
    series.map((v, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)} ${y(v).toFixed(1)}`).join(' ');

  const line = path(values);
  const lastIndex = values.length - 1;
  return {
    line,
    area: `${line} L${x(lastIndex).toFixed(1)} ${height} L${x(0).toFixed(1)} ${height}Z`,
    reference: path(reference.slice(0, values.length)),
    last: { x: x(lastIndex), y: y(values[lastIndex]) },
  };
}
