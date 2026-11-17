import { beforeEach, describe, expect, it } from 'vitest';
import { SKIN_STORAGE_KEY, loadSkin, saveSkin, skinInfo } from './skin';

describe('стиль оформления', () => {
  beforeEach(() => localStorage.clear());

  it('по умолчанию — «Классика»', () => {
    expect(loadSkin()).toBe('classic');
  });

  it('сохраняет и читает выбор', () => {
    saveSkin('metro');
    expect(loadSkin()).toBe('metro');
  });

  it('мусор или удалённый стиль в хранилище — «Классика»', () => {
    localStorage.setItem(SKIN_STORAGE_KEY, 'poster');
    expect(loadSkin()).toBe('classic');
  });

  it('«Стекло» и «Тетрадь» только светлые, «Метро» только тёмное, «Классика» — любая', () => {
    expect(skinInfo('glass').fixedTheme).toBe('light');
    expect(skinInfo('notebook').fixedTheme).toBe('light');
    expect(skinInfo('metro').fixedTheme).toBe('dark');
    expect(skinInfo('classic').fixedTheme).toBeNull();
  });
});
