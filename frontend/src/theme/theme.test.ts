import { beforeEach, describe, expect, it } from 'vitest';
import { loadThemeChoice, resolveTheme, saveThemeChoice, THEME_STORAGE_KEY } from './theme';

describe('resolveTheme', () => {
  it('явный выбор важнее системы', () => {
    expect(resolveTheme('light', true)).toBe('light');
    expect(resolveTheme('dark', false)).toBe('dark');
  });

  it('"Как в системе" следует настройке ОС', () => {
    expect(resolveTheme('system', true)).toBe('dark');
    expect(resolveTheme('system', false)).toBe('light');
  });
});

describe('сохранение выбора', () => {
  beforeEach(() => localStorage.clear());

  it('по умолчанию — как в системе', () => {
    expect(loadThemeChoice()).toBe('system');
  });

  it('сохраняет и читает выбор', () => {
    saveThemeChoice('dark');
    expect(loadThemeChoice()).toBe('dark');
  });

  it('мусор в хранилище не ломает приложение', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'purple');
    expect(loadThemeChoice()).toBe('system');
  });
});
