/** Флаг «Диагностика экрана» — в этом браузере (localStorage). ВРЕМЕННО, вместе с LayoutDebug. */
const KEY = 'vegas.debug.layout';

export function loadLayoutDebug(): boolean {
  try {
    return localStorage.getItem(KEY) === '1';
  } catch {
    return false;
  }
}

export function saveLayoutDebug(on: boolean): void {
  try {
    if (on) localStorage.setItem(KEY, '1');
    else localStorage.removeItem(KEY);
  } catch {
    // не сохранилось — действует до перезагрузки
  }
}
