import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, configureApiClient, refreshAccessToken } from './client';

function mockFetch(status: number, body?: unknown) {
  const fetchMock = vi
    .fn()
    .mockResolvedValue(new Response(body === undefined ? null : JSON.stringify(body), { status }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  configureApiClient({ getToken: () => null, onUnauthorized: () => {} });
});

describe('api', () => {
  it('подставляет токен и разбирает JSON', async () => {
    const fetchMock = mockFetch(200, { id: '1' });
    configureApiClient({ getToken: () => 'abc', onUnauthorized: () => {} });

    await expect(api('/api/users/me')).resolves.toEqual({ id: '1' });
    const [, init] = fetchMock.mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer abc');
  });

  it('показывает сообщение из ErrorResponse бэкенда', async () => {
    mockFetch(400, { status: 400, error: 'Bad Request', message: 'password: пароль должен быть от 8 до 72 символов' });

    await expect(api('/api/auth/register', { method: 'POST', body: {} })).rejects.toEqual(
      new ApiError(400, 'password: пароль должен быть от 8 до 72 символов'),
    );
  });

  it('401 и refresh не удался — разлогинивает', async () => {
    const fetchMock = mockFetch(401);
    const onUnauthorized = vi.fn();
    configureApiClient({ getToken: () => 'expired', onUnauthorized });

    await expect(api('/api/users/me')).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledOnce();
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh');
  });

  it('401 — молча обновляет токен по cookie и повторяет запрос', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: 'fresh' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: '1' }), { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    const onTokenRefreshed = vi.fn();
    const onUnauthorized = vi.fn();
    configureApiClient({ getToken: () => 'expired', onTokenRefreshed, onUnauthorized });

    await expect(api('/api/users/me')).resolves.toEqual({ id: '1' });
    expect(onTokenRefreshed).toHaveBeenCalledWith('fresh');
    expect(onUnauthorized).not.toHaveBeenCalled();
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer fresh');
  });

  it('401 на логине — это неверный пароль, а не истёкший токен: refresh не вызываем', async () => {
    const fetchMock = mockFetch(401, { status: 401, error: 'Unauthorized', message: 'Неверный email или пароль' });
    configureApiClient({ getToken: () => 'old', onUnauthorized: () => {} });

    await expect(api('/api/auth/login', { method: 'POST', body: {} })).rejects.toHaveProperty(
      'message',
      'Неверный email или пароль',
    );
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('параллельные обновления — один запрос refresh на всех', async () => {
    const fetchMock = mockFetch(200, { accessToken: 'fresh' });

    const tokens = await Promise.all([refreshAccessToken(), refreshAccessToken(), refreshAccessToken()]);

    expect(tokens).toEqual(['fresh', 'fresh', 'fresh']);
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('сервис не запущен — понятное сообщение', async () => {
    mockFetch(504);
    await expect(api('/api/users/me')).rejects.toHaveProperty(
      'message',
      'Сервис временно недоступен. Попробуйте позже.',
    );
  });
});
