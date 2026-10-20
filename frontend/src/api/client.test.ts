import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, configureApiClient } from './client';

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

  it('401 с токеном — разлогинивает', async () => {
    mockFetch(401);
    const onUnauthorized = vi.fn();
    configureApiClient({ getToken: () => 'expired', onUnauthorized });

    await expect(api('/api/users/me')).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('сервис не запущен — понятное сообщение', async () => {
    mockFetch(504);
    await expect(api('/api/users/me')).rejects.toHaveProperty(
      'message',
      'Сервис временно недоступен. Попробуйте позже.',
    );
  });
});
