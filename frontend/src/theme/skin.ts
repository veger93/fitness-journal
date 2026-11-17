/**
 * Стиль оформления (docs/styles.md): «Классика» — как было, и пробные стили, которые сравниваем в деле.
 * Тема (светлая/тёмная) и стиль — разные вещи: стиль меняет шрифты, цвета и детали, тема — светло или темно.
 * Стиль, у которого нарисован только один вариант, тему фиксирует (fixedTheme).
 *
 * Новый стиль = строка здесь + файл styles/skins/<id>.css + шрифты в SKIN_FONTS + ветка в index.html.
 */
import type { Theme } from './theme';

export type Skin = 'classic' | 'glass' | 'notebook' | 'metro';

export interface SkinInfo {
  id: Skin;
  title: string;
  description: string;
  /** у стиля только этот вариант — переключатель темы для него не показываем */
  fixedTheme: Theme | null;
  /** цвет полосы браузера (meta theme-color), если тема фиксирована */
  themeColor: string | null;
}

export const SKINS: SkinInfo[] = [
  { id: 'classic', title: 'Классика', description: 'как было: строгий и светлый', fixedTheme: null, themeColor: null },
  {
    id: 'glass',
    title: 'Стекло',
    description: 'светлый воздух, матовое стекло, цветные блики',
    fixedTheme: 'light',
    themeColor: '#eaeef6',
  },
  {
    id: 'notebook',
    title: 'Тетрадь',
    description: 'клетка, ручка, маркер и стикеры',
    fixedTheme: 'light',
    themeColor: '#fbfaf5',
  },
  {
    id: 'metro',
    title: 'Метро',
    description: 'чёрный и жёлтый, как в зале',
    fixedTheme: 'dark',
    themeColor: '#0a0a0a',
  },
];

/** Шрифты стиля — грузим только когда стиль выбран: «Классике» лишние килобайты не нужны */
export const SKIN_FONTS: Partial<Record<Skin, string>> = {
  notebook:
    'https://fonts.googleapis.com/css2?family=Caveat:wght@600;700&family=Golos+Text:wght@400;500;600;700&display=swap',
  glass: 'https://fonts.googleapis.com/css2?family=Manrope:wght@500;600;700;800&display=swap',
  // Onest и Unbounded 500/700 уже есть (index.html), «Метро» нужна жирная 800 для заголовков
  metro: 'https://fonts.googleapis.com/css2?family=Unbounded:wght@800&display=swap',
};

export const SKIN_STORAGE_KEY = 'vegas.skin';

export function isSkin(value: unknown): value is Skin {
  return SKINS.some((s) => s.id === value);
}

export function skinInfo(skin: Skin): SkinInfo {
  return SKINS.find((s) => s.id === skin) ?? SKINS[0];
}

/** localStorage может быть недоступен (приватный режим) — тогда «Классика» */
export function loadSkin(): Skin {
  try {
    const saved = localStorage.getItem(SKIN_STORAGE_KEY);
    return isSkin(saved) ? saved : 'classic';
  } catch {
    return 'classic';
  }
}

export function saveSkin(skin: Skin): void {
  try {
    localStorage.setItem(SKIN_STORAGE_KEY, skin);
  } catch {
    // не сохранилось — стиль действует до перезагрузки
  }
}
