/**
 * Стиль оформления (docs/styles.md): «Классика» — как было, и пробные стили, которые сравниваем в деле.
 * Тема (светлая/тёмная) и стиль — разные вещи: стиль меняет шрифты, цвета и детали, тема — светло или темно.
 * Стиль, у которого пока есть только светлый вариант, тёмную тему отключает (forcesLight).
 *
 * Новый стиль = строка здесь + файл styles/skins/<id>.css + шрифты в SKIN_FONTS.
 */
export type Skin = 'classic' | 'notebook';

export interface SkinInfo {
  id: Skin;
  title: string;
  description: string;
  /** только светлый вариант: тёмная тема с этим стилем не нарисована */
  forcesLight: boolean;
  /** цвет полосы браузера (meta theme-color) в светлой теме — фон страницы */
  themeColor: string;
}

export const SKINS: SkinInfo[] = [
  {
    id: 'classic',
    title: 'Классика',
    description: 'как было: строгий и светлый',
    forcesLight: false,
    themeColor: '#eef1f5',
  },
  {
    id: 'notebook',
    title: 'Тетрадь',
    description: 'клетка, ручка, маркер и стикеры',
    forcesLight: true,
    themeColor: '#fbfaf5',
  },
];

/** Шрифты стиля — грузим только когда стиль выбран: «Классике» лишние килобайты не нужны */
export const SKIN_FONTS: Partial<Record<Skin, string>> = {
  notebook:
    'https://fonts.googleapis.com/css2?family=Caveat:wght@600;700&family=Golos+Text:wght@400;500;600;700&display=swap',
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
