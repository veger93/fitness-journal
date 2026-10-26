import type { ProgressPoint } from '../api/analytics';
import type { TrackingType } from '../api/workouts';
import { formatKg, formatSet } from './format';
import { formatClock } from './workout';

/** Строка истории на странице упражнения: одна тренировка. */
export interface HistoryRow {
  date: string;
  /** лучший подход: "80 кг × 5", "12 повт.", "1:30" */
  best: string;
  /** "3 подхода" */
  sets: string;
  /** "93,3 кг" — только у силовых с весом */
  strength: string | null;
}

/**
 * История по тренировкам — свежие сверху.
 * Сервер отдаёт точки по возрастанию даты (для графика), поэтому разворачиваем копию: reverse() меняет массив на месте,
 * а это данные из кэша TanStack Query — их трогать нельзя.
 */
export function historyRows(points: ProgressPoint[], trackingType: TrackingType): HistoryRow[] {
  return [...points].reverse().map((point) => ({
    date: point.date,
    best: bestResult(point, trackingType),
    sets: setsLabel(point.workingSets),
    strength: point.e1rmKg != null ? formatKg(point.e1rmKg) : null,
  }));
}

function bestResult(point: ProgressPoint, trackingType: TrackingType): string {
  if (trackingType === 'TIME') {
    return point.bestDurationSec != null ? formatClock(point.bestDurationSec) : '—';
  }
  return formatSet(point.bestWeightKg, point.bestReps) ?? '—';
}

/** 1 подход, 2 подхода, 5 подходов, 11 подходов, 21 подход */
export function setsLabel(count: number): string {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) {
    return `${count} подход`;
  }
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
    return `${count} подхода`;
  }
  return `${count} подходов`;
}

/** Лучшее время за период — у упражнений на время это главная цифра вместо силы. */
export function bestDuration(points: ProgressPoint[]): number | null {
  const durations = points.map((p) => p.bestDurationSec).filter((d): d is number => d != null);
  return durations.length > 0 ? Math.max(...durations) : null;
}
