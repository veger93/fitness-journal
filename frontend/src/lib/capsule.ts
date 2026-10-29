/**
 * Математика "капли" в нижней панели: светлая капсула перетекает с одной вкладки на другую.
 * Чистые функции без DOM — их легко проверить тестами (capsule.test.ts).
 */

/** Горизонтальные границы капсулы в пикселях внутри панели */
export interface Span {
  left: number;
  right: number;
}

/** Длительность перетекания, мс */
export const CAPSULE_DURATION = 450;

const clamp01 = (x: number) => Math.min(Math.max(x, 0), 1);
const easeOutCubic = (x: number) => 1 - (1 - x) ** 3;
const easeInOutCubic = (x: number) => (x < 0.5 ? 4 * x ** 3 : 1 - (-2 * x + 2) ** 3 / 2);

/**
 * Где капсула через долю времени t (0..1) по пути from → to.
 *
 * Эффект капли: передний край (по направлению движения) срывается сразу и быстро,
 * задний догоняет с задержкой — капсула вытягивается, а у цели снова собирается.
 * Пока вытянута — чуть сплющивается по высоте (squash 0..0.12), как капля в движении.
 */
export function capsuleFrame(from: Span, to: Span, t: number): Span & { squash: number } {
  const lead = easeOutCubic(clamp01(t / 0.7));
  const trail = easeInOutCubic(clamp01((t - 0.15) / 0.85));
  const movingRight = to.left >= from.left;

  const left = from.left + (to.left - from.left) * (movingRight ? trail : lead);
  const right = from.right + (to.right - from.right) * (movingRight ? lead : trail);

  // насколько капсула шире, чем "в покое" (ширина плавно меняется от старой вкладки к новой): 0 — не вытянута
  const restWidth = Math.max(from.right - from.left + (to.right - to.left - (from.right - from.left)) * t, 1);
  const stretch = Math.max((right - left) / restWidth - 1, 0);
  const squash = Math.min(stretch * 0.08, 0.12);

  return { left, right, squash };
}
