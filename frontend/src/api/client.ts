/**
 * Обёртка над fetch: один раз решаем то, что иначе повторялось бы в каждом запросе:
 * - подставляем access-токен в заголовок Authorization;
 * - на 401 молча обновляем токен по refresh-cookie и повторяем запрос; не вышло — "разлогиниваем";
 * - превращаем ошибку бэкенда (ErrorResponse из common-lib) в понятный ApiError.
 *
 * Access-токен живёт только в памяти (переменная в AuthProvider), не в localStorage: после перезагрузки
 * страницы его нет, и AuthProvider получает новый через /api/auth/refresh. Refresh-токен лежит в
 * httpOnly-cookie — JavaScript его не видит вообще, браузер сам отправляет его на /api/auth/*.
 */

/** Формат ошибки всех сервисов: com.vegas.common.dto.ErrorResponse */
interface ErrorResponse {
  status: number;
  error: string;
  message: string;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface ClientOptions {
  getToken: () => string | null;
  /** токен обновили по refresh-cookie — AuthProvider запоминает новый */
  onTokenRefreshed?: (token: string) => void;
  /** обновить не получилось (cookie нет, истёк, отозван) — пора на экран входа */
  onUnauthorized: () => void;
}

let options: ClientOptions = { getToken: () => null, onUnauthorized: () => {} };

/** AuthProvider сообщает клиенту, где брать токен и что делать при 401. */
export function configureApiClient(next: ClientOptions) {
  options = next;
}

let refreshing: Promise<string | null> | null = null;

/**
 * Новый access-token по refresh-cookie. null — войти без пароля не получилось.
 * Single-flight: если 5 запросов одновременно получили 401, обновление уйдёт ОДНО, остальные подождут его же.
 * Иначе пять параллельных refresh с одной cookie — и сервер принял бы это за кражу токена.
 */
export function refreshAccessToken(): Promise<string | null> {
  refreshing ??= (async () => {
    try {
      // cookie браузер приложит сам: запрос на тот же адрес (same-origin), путь /api/auth
      const response = await fetch('/api/auth/refresh', { method: 'POST', headers: { Accept: 'application/json' } });
      if (!response.ok) {
        return null;
      }
      const body = (await response.json()) as { accessToken: string };
      return body.accessToken;
    } catch {
      return null;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
}

export async function api<T>(path: string, { method = 'GET', body }: RequestOptions = {}): Promise<T> {
  const token = options.getToken();
  let response = await send(path, method, body, token);

  // access-токен истёк (живёт 15 минут) — обновляем и повторяем запрос один раз.
  // Эндпоинты /api/auth/* не трогаем: 401 на логине — это "неверный пароль", а не истёкший токен.
  if (response.status === 401 && token && !path.startsWith('/api/auth/')) {
    const fresh = await refreshAccessToken();
    if (fresh) {
      options.onTokenRefreshed?.(fresh);
      response = await send(path, method, body, fresh);
    }
    if (!fresh || response.status === 401) {
      options.onUnauthorized();
    }
  }
  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response));
  }
  // 204 No Content и пустые ответы
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function send(path: string, method: string, body: unknown, token: string | null): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  try {
    return await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Нет связи с сервером. Проверьте, что gateway запущен.');
  }
}

async function errorMessage(response: Response): Promise<string> {
  // 502/503/504 отдаёт прокси Vite или gateway, когда нужный сервис не запущен
  if (response.status >= 502 && response.status <= 504) {
    return 'Сервис временно недоступен. Попробуйте позже.';
  }
  try {
    const error = (await response.json()) as Partial<ErrorResponse>;
    if (error.message) {
      return error.message;
    }
  } catch {
    // тело не JSON — ниже общее сообщение
  }
  return `Ошибка ${response.status}`;
}
