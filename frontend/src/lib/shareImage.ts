import { formatChange, formatKg } from './format';

/** Пост (4:5 — лента Telegram/Instagram) или история (9:16 — во весь экран телефона) */
export type ShareFormat = 'POST' | 'STORY';

export const SHARE_SIZES: Record<ShareFormat, { width: number; height: number }> = {
  POST: { width: 1080, height: 1350 },
  STORY: { width: 1080, height: 1920 },
};

export interface ShareChartData {
  exerciseName: string;
  /** "за 3 месяца" */
  periodCaption: string;
  /** сила (лучшее за 4 недели); null — у упражнения нет силы */
  strengthKg: number | null;
  /** рост силы за период, % */
  changePercent: number | null;
  /** сила по тренировкам, от старых к новым — линия графика */
  points: number[];
  record: boolean;
  /** false — без килограммов: только % роста (кто не хочет показывать свои веса) */
  showWeight: boolean;
  format: ShareFormat;
}

const PADDING = 88;
const BG = '#111827';
const TEXT = '#ffffff';
const MUTED = 'rgba(255, 255, 255, 0.6)';
const ACCENT = '#fb923c';
const GROWTH = '#4ade80';
const DISPLAY = 'Unbounded, system-ui, sans-serif';
const BODY = 'Onest, system-ui, sans-serif';

/**
 * Название в 1–2 строки: сначала пробуем крупный шрифт, не влезает в 2 строки — шрифт меньше.
 * Совсем длинное ("Сгибания с гантелями на наклонной скамье" при самом мелком шрифте) — вторая строка с "…".
 * measure — ширина текста в пикселях при данном размере шрифта (в браузере — canvas.measureText).
 */
export function fitTitle(
  text: string,
  maxWidth: number,
  measure: (text: string, size: number) => number,
  sizes: number[] = [76, 64, 54],
): { size: number; lines: string[] } {
  for (const size of sizes) {
    const lines = wrap(text, maxWidth, (t) => measure(t, size));
    if (lines.length <= 2) {
      return { size, lines };
    }
  }
  const size = sizes[sizes.length - 1];
  const lines = wrap(text, maxWidth, (t) => measure(t, size));
  let second = lines.slice(1).join(' ');
  while (second.length > 1 && measure(`${second}…`, size) > maxWidth) {
    second = second.slice(0, -1).trimEnd();
  }
  return { size, lines: [lines[0], `${second}…`] };
}

/** Перенос по словам; слово длиннее строки остаётся целым (лучше чуть вылезти, чем рвать слово) */
function wrap(text: string, maxWidth: number, measure: (text: string) => number): string[] {
  const lines: string[] = [];
  let line = '';
  for (const word of text.split(/\s+/).filter(Boolean)) {
    const candidate = line ? `${line} ${word}` : word;
    if (line && measure(candidate) > maxWidth) {
      lines.push(line);
      line = word;
    } else {
      line = candidate;
    }
  }
  if (line) {
    lines.push(line);
  }
  return lines;
}

/** Главная цифра: "102 кг"; без веса — рост "+18%" (или прочерк, если роста не посчитать) */
export function headline(data: Pick<ShareChartData, 'strengthKg' | 'changePercent' | 'showWeight'>): {
  value: string;
  growth: string | null;
  caption: string;
} {
  const growth = data.changePercent != null ? formatChange(data.changePercent) : null;
  if (data.showWeight && data.strengthKg != null) {
    return { value: formatKg(data.strengthKg), growth, caption: 'сила · сколько поднял бы один раз' };
  }
  return { value: growth ?? '—', growth: null, caption: 'рост силы' };
}

/**
 * Рисует картинку прямо в браузере (canvas) и отдаёт PNG. Сервер не нужен, данные никуда не уходят —
 * человек сам решает, кому отправить картинку. Шрифты — те же, что в приложении (ждём их загрузки,
 * иначе canvas нарисовал бы системным шрифтом).
 */
export async function renderShareImage(data: ShareChartData): Promise<Blob> {
  const { width, height } = SHARE_SIZES[data.format];
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('Не получилось нарисовать картинку');
  }
  await loadFonts();

  ctx.fillStyle = BG;
  ctx.fillRect(0, 0, width, height);
  const inner = width - PADDING * 2;
  let y = PADDING;

  ctx.textBaseline = 'top';
  ctx.fillStyle = TEXT;
  ctx.font = `700 40px ${DISPLAY}`;
  ctx.fillText('VEGAS', PADDING, y);
  y += 40 + (data.format === 'STORY' ? 140 : 72);

  const title = fitTitle(data.exerciseName, inner, (text, size) => {
    ctx.font = `700 ${size}px ${DISPLAY}`;
    return ctx.measureText(text).width;
  });
  ctx.font = `700 ${title.size}px ${DISPLAY}`;
  for (const line of title.lines) {
    ctx.fillText(line, PADDING, y);
    y += title.size * 1.18;
  }
  y += 8;
  ctx.fillStyle = MUTED;
  ctx.font = `400 36px ${BODY}`;
  ctx.fillText(data.periodCaption, PADDING, y);
  y += 36 + 56;

  const head = headline(data);
  ctx.fillStyle = TEXT;
  ctx.font = `700 132px ${DISPLAY}`;
  ctx.fillText(head.value, PADDING, y);
  if (head.growth) {
    const valueWidth = ctx.measureText(head.value).width;
    ctx.fillStyle = GROWTH;
    ctx.font = `700 56px ${DISPLAY}`;
    ctx.fillText(head.growth, PADDING + valueWidth + 28, y + 62);
  }
  y += 132 + 20;
  ctx.fillStyle = MUTED;
  ctx.font = `400 32px ${BODY}`;
  ctx.fillText(head.caption, PADDING, y);
  y += 32 + 72;

  // график: от y до нижней подписи
  const footerTop = height - PADDING - 32;
  const recordHeight = data.record ? 40 + 40 : 0;
  const chartBottom = footerTop - 56 - recordHeight;
  drawLine(ctx, data.points, PADDING, y, inner, chartBottom - y);

  if (data.record) {
    ctx.fillStyle = ACCENT;
    ctx.font = `600 40px ${BODY}`;
    ctx.fillText('★ Новый рекорд', PADDING, chartBottom + 40);
  }
  ctx.fillStyle = MUTED;
  ctx.font = `400 32px ${BODY}`;
  ctx.fillText('vegas-fit.ru', PADDING, footerTop);

  return new Promise((resolve, reject) =>
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('Не получилось сохранить картинку'))),
      'image/png',
    ),
  );
}

/** Линия силы в прямоугольнике: масштаб по своим min/max с запасом, последняя точка — кружок */
function drawLine(ctx: CanvasRenderingContext2D, points: number[], x: number, y: number, w: number, h: number) {
  if (points.length < 2 || h <= 0) {
    return;
  }
  const min = Math.min(...points);
  const max = Math.max(...points);
  const span = max - min || 1;
  const pad = 24;
  const toX = (i: number) => x + pad + (i / (points.length - 1)) * (w - pad * 2);
  const toY = (v: number) => y + pad + (1 - (v - min) / span) * (h - pad * 2);

  ctx.strokeStyle = ACCENT;
  ctx.lineWidth = 10;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.beginPath();
  points.forEach((v, i) => (i === 0 ? ctx.moveTo(toX(i), toY(v)) : ctx.lineTo(toX(i), toY(v))));
  ctx.stroke();

  const last = points.length - 1;
  ctx.fillStyle = ACCENT;
  ctx.beginPath();
  ctx.arc(toX(last), toY(points[last]), 16, 0, Math.PI * 2);
  ctx.fill();
}

async function loadFonts() {
  try {
    await Promise.all([
      document.fonts.load(`700 76px ${DISPLAY}`),
      document.fonts.load(`400 36px ${BODY}`),
      document.fonts.load(`600 40px ${BODY}`),
    ]);
  } catch {
    // шрифт не загрузился — нарисуем системным, картинка всё равно получится
  }
}
