/**
 * Выбор пользователя ('system' = как в настройках телефона/компьютера)
 * и реальная тема, которую рисуем ('light' | 'dark').
 */
export type ThemeChoice = 'light' | 'dark' | 'system';
export type Theme = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'vegas.theme';

/** Чистая функция: никакого DOM, легко тестировать. */
export function resolveTheme(choice: ThemeChoice, systemPrefersDark: boolean): Theme {
  if (choice === 'system') {
    return systemPrefersDark ? 'dark' : 'light';
  }
  return choice;
}

export function isThemeChoice(value: unknown): value is ThemeChoice {
  return value === 'light' || value === 'dark' || value === 'system';
}

/** localStorage может быть недоступен (приватный режим) — тогда просто 'system'. */
export function loadThemeChoice(): ThemeChoice {
  try {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    return isThemeChoice(saved) ? saved : 'system';
  } catch {
    return 'system';
  }
}

export function saveThemeChoice(choice: ThemeChoice): void {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, choice);
  } catch {
    // не сохранилось — не страшно, выбор действует до перезагрузки
  }
}
