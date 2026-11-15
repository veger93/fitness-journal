import type {
  BodyLimit,
  PriorityGroup,
  Program,
  ProgramDay,
  ProgramGoal,
  ProgramLocation,
  ProgramSlot,
} from '../api/programs';
import type { DayOfWeek } from '../api/workouts';
import { plural } from './format';
import { DAYS } from './plan';

/**
 * Тексты анкеты и экрана «Ваша программа» (docs/program.md). Без React — чтобы покрыть тестами и держать
 * все формулировки в одном месте: правила генератора и то, как мы их объясняем человеку, должны совпадать.
 */

export const GOALS: { value: ProgramGoal; title: string; description: string; short: string }[] = [
  {
    value: 'MUSCLE',
    title: 'Набрать мышцы',
    description: '8–12 повторов, объём растёт постепенно',
    short: 'Набрать мышцы',
  },
  { value: 'STRENGTH', title: 'Стать сильнее', description: 'Базовые упражнения на 4–6 повторов', short: 'Сила' },
  {
    value: 'FAT_LOSS',
    title: 'Похудеть и подтянуться',
    description: 'Силовые сохраняют мышцы, плюс совет по кардио',
    short: 'Похудеть',
  },
];

export const DAY_OPTIONS = [2, 3, 4, 5] as const;
export const MINUTE_OPTIONS = [
  { value: '45', label: '~45 мин' },
  { value: '60', label: '~60 мин' },
  { value: '75', label: '75+ мин' },
] as const;

/** П1: что получится при таком числе дней — показываем под выбором */
export function splitHint(days: number): { title: string; text: string } {
  switch (days) {
    case 2:
      return {
        title: '2 дня — всё тело дважды',
        text: 'Каждая мышца работает 2 раза в неделю. Минимум для новичка — больше даст 3-й день или 75 минут.',
      };
    case 3:
      return {
        title: '3 дня — всё тело каждую тренировку',
        text: 'Каждая мышца работает 3 раза в неделю. Новичкам рекомендуют 2–3 дня*.',
      };
    case 4:
      return { title: '4 дня — верх / низ', text: 'Верх и низ по два раза: каждая мышца — 2 раза в неделю.' };
    default:
      return {
        title: '5 дней — толкай / тяни / ноги + верх / низ',
        text: 'Для тех, кто тренируется давно: больше объёма, каждая мышца — 2 раза в неделю.',
      };
  }
}

/** П9: время → упражнений за тренировку (как в генераторе: 45 → 5, 60 → 6, 75 → 7) */
export function minutesHint(minutes: number): string {
  const exercises = minutes <= 45 ? 5 : minutes <= 60 ? 6 : 7;
  return `≈ ${plural(exercises, 'упражнение', 'упражнения', 'упражнений')} за тренировку.`;
}

export const LOCATIONS: { value: ProgramLocation; title: string; description: string; short: string }[] = [
  { value: 'GYM', title: 'Зал', description: 'штанга, гантели, тренажёры', short: 'зал' },
  { value: 'HOME_DUMBBELLS', title: 'Дома', description: 'есть гантели', short: 'дома' },
];

/** П13 — что именно исключим. Честно и конкретно; «не медицинский совет» — один раз под списком */
export const LIMITS: { value: BodyLimit; label: string; excludes: string }[] = [
  { value: 'KNEES', label: 'Колени', excludes: 'колени — без выпадов и приседа со штангой' },
  {
    value: 'LOWER_BACK',
    label: 'Поясница',
    excludes:
      'поясница — без наклонов с весом (становая, румынская тяга), тяги в наклоне, приседа и жима стоя со штангой',
  },
  { value: 'SHOULDERS', label: 'Плечи', excludes: 'плечи — без жима штанги над головой, брусьев и тяги к подбородку' },
];

export const PRIORITIES: { value: PriorityGroup | null; label: string }[] = [
  { value: null, label: 'Всё ровно' },
  { value: 'CHEST', label: 'Грудь' },
  { value: 'BACK', label: 'Спина' },
  { value: 'LEGS', label: 'Ноги и ягодицы' },
  { value: 'ARMS', label: 'Руки' },
  { value: 'SHOULDERS', label: 'Плечи' },
];

/** Безопасность — по мотивам PAR-Q+. Ответы не отправляем и не храним: это данные о здоровье */
export const SAFETY_QUESTIONS = [
  'Бывает боль в груди при нагрузке?',
  'Теряли сознание или сильно кружилась голова?',
  'Врач говорил о проблемах с сердцем или давлением?',
  'Врач советовал ограничить нагрузки?',
] as const;

// ---------- экран «Ваша программа» ----------

/** «Набрать мышцы · 3 дня · ~60 мин · зал» */
export function programSummary(p: Pick<Program, 'goal' | 'daysPerWeek' | 'sessionMinutes' | 'location'>): string {
  const goal = GOALS.find((g) => g.value === p.goal)?.title ?? '';
  const minutes = p.sessionMinutes >= 75 ? '75+ мин' : `~${p.sessionMinutes} мин`;
  const place = LOCATIONS.find((l) => l.value === p.location)?.short ?? '';
  return [goal, plural(p.daysPerWeek, 'день', 'дня', 'дней'), minutes, place].join(' · ');
}

/** 120 → «2 мин», 90 → «1,5 мин», 45 → «45 с» */
export function formatRest(sec: number): string {
  if (sec < 60) {
    return `${sec} с`;
  }
  return `${String(sec / 60).replace('.', ',')} мин`;
}

/** «3 × 8–12 · отдых 2 мин» */
export function slotScheme(slot: Pick<ProgramSlot, 'sets' | 'repsMin' | 'repsMax' | 'restSec'>): string {
  const reps = slot.repsMin === slot.repsMax ? `${slot.repsMin}` : `${slot.repsMin}–${slot.repsMax}`;
  return `${slot.sets} × ${reps} · отдых ${formatRest(slot.restSec)}`;
}

/** «Пн · Пт»; пусто — день убран из плана */
export function weekDaysText(days: DayOfWeek[]): string {
  return DAYS.filter((d) => days.includes(d.day))
    .map((d) => d.short)
    .join(' · ');
}

/** Дни недели без тренировок программы: «Вт, Чт, Сб, Вс» */
export function restDaysText(days: ProgramDay[]): string {
  const busy = new Set(days.flatMap((d) => d.weekDays));
  return DAYS.filter((d) => !busy.has(d.day))
    .map((d) => d.short)
    .join(', ');
}

/** Первые упражнения дня одной строкой: «присед, жим лёжа, тяга верхнего блока +3» */
export function dayPreview(day: ProgramDay, shown = 3): string {
  const names = day.slots.slice(0, shown).map((s) => s.exerciseName.toLowerCase());
  const more = day.slots.length - shown;
  return more > 0 ? `${names.join(', ')} +${more}` : names.join(', ');
}

/** Неделя программы: 1-я — первые 7 дней со старта. Не больше 6 — дальше пора пересматривать */
export function programWeek(startedAt: string, now: Date = new Date()): number {
  const days = Math.floor((now.getTime() - new Date(startedAt).getTime()) / 86_400_000);
  return Math.min(6, Math.max(1, Math.floor(days / 7) + 1));
}

/** Главные цифры для плитки: повторы основного упражнения и отдых в базовых — с первого места первого дня */
export function headline(program: Program): { exercises: number; reps: string; rest: string } | null {
  const first = program.days[0]?.slots[0];
  if (!first) {
    return null;
  }
  return {
    exercises: Math.max(...program.days.map((d) => d.slots.length)),
    reps: first.repsMin === first.repsMax ? `${first.repsMin}` : `${first.repsMin}–${first.repsMax}`,
    rest: formatRest(first.restSec),
  };
}

export interface Reason {
  title: string;
  text: string;
  source: string;
}

/** «Почему так»: те же правила П1, П4, П10, П12 (docs/program.md), что и в генераторе — короткими словами */
export function programReasons(p: Pick<Program, 'goal' | 'daysPerWeek'>): Reason[] {
  const timesPerWeek = p.daysPerWeek === 3 ? 3 : 2;
  const reasons: Reason[] = [
    {
      title: `Каждая мышца — ${plural(timesPerWeek, 'раз', 'раза', 'раз')} в неделю`,
      text:
        p.daysPerWeek <= 3
          ? 'Тренировки всего тела: так советуют начинать, а 2 раза в неделю на мышцу лучше одного.'
          : 'Нагрузка разделена по дням, но каждая мышца всё равно работает дважды — это лучше одного раза.',
      source: 'ACSM 2009 · Schoenfeld 2016',
    },
  ];
  switch (p.goal) {
    case 'STRENGTH':
      reasons.push({
        title: 'В базовых — 4–6 повторов',
        text: 'Сила растёт от тяжёлых подходов; остальные упражнения — 8–12 повторов, оставляйте 1–3 в запасе.',
        source: 'ACSM 2009 · ACSM 2026',
      });
      break;
    case 'FAT_LOSS':
      reasons.push({
        title: '10–15 повторов и кардио',
        text: 'Силовые сохраняют мышцы, пока уходит жир. Добавьте 150–300 минут умеренного кардио в неделю.',
        source: 'ВОЗ 2020',
      });
      break;
    default:
      reasons.push({
        title: '8–12 повторов, 1–3 в запасе',
        text: 'До отказа не обязательно: рост мышц почти тот же, а техника и восстановление лучше.',
        source: 'ACSM 2009 · Robinson 2024',
      });
  }
  reasons.push({
    title: 'Дошли до верхней границы — прибавьте вес',
    text: 'Два раза подряд во всех подходах сделали верхнее число повторов — добавьте шаг веса и начните снизу.',
    source: 'ACSM 2009',
  });
  return reasons;
}
