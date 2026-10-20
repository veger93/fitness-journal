/**
 * Где храним токен: localStorage. Просто и переживает перезагрузку страницы.
 * Минус: токен доступен любому скрипту на странице (XSS). Надёжнее — httpOnly-cookie,
 * но это доработка бэкенда; для учебного MVP осознанно берём localStorage.
 */
const SESSION_KEY = 'vegas.session';

export interface Session {
  token: string;
  expiresAt: number; // ms, Date.now()-шкала
}

export function loadSession(now = Date.now()): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    return session.expiresAt > now ? session : null; // истёкший токен даже не пробуем
  } catch {
    return null;
  }
}

export function saveSession(session: Session | null): void {
  try {
    if (session) {
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(SESSION_KEY);
    }
  } catch {
    // нет доступа к хранилищу — сессия живёт до перезагрузки
  }
}
