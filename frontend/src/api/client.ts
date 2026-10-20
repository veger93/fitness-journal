/**
 * Обёртка над fetch: один раз решаем то, что иначе повторялось бы в каждом запросе:
 * - подставляем JWT в заголовок Authorization;
 * - превращаем ошибку бэкенда (ErrorResponse из common-lib) в понятный ApiError;
 * - на 401 "разлогиниваем" пользователя (токен истёк или битый).
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

let getToken: () => string | null = () => null;
let onUnauthorized: () => void = () => {};

/** AuthProvider сообщает клиенту, где брать токен и что делать при 401. */
export function configureApiClient(options: { getToken: () => string | null; onUnauthorized: () => void }) {
  getToken = options.getToken;
  onUnauthorized = options.onUnauthorized;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
}

export async function api<T>(path: string, { method = 'GET', body }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Нет связи с сервером. Проверьте, что gateway запущен.');
  }

  if (response.status === 401 && token) {
    onUnauthorized();
  }
  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response));
  }
  // 204 No Content и пустые ответы
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
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
