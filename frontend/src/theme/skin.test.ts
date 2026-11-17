import { beforeEach, describe, expect, it } from 'vitest';
import { SKIN_STORAGE_KEY, loadSkin, saveSkin, skinInfo } from './skin';

describe('стиль оформления', () => {
  beforeEach(() => localStorage.clear());

  it('по умолчанию — «Классика»', () => {
    expect(loadSkin()).toBe('classic');
  });

  it('сохраняет и читает выбор', () => {
    saveSkin('notebook');
    expect(loadSkin()).toBe('notebook');
  });

  it('мусор или удалённый стиль в хранилище — «Классика»', () => {
    localStorage.setItem(SKIN_STORAGE_KEY, 'poster');
    expect(loadSkin()).toBe('classic');
  });

  it('«Тетрадь» пока только светлая', () => {
    expect(skinInfo('notebook').forcesLight).toBe(true);
    expect(skinInfo('classic').forcesLight).toBe(false);
  });
});
