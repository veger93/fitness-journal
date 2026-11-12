import { describe, expect, it } from 'vitest';
import { ApiError, NO_RESPONSE } from '../api/client';
import { isRetryable, newClientId } from './useWorkout';

describe('запись подхода: когда повторять', () => {
  it('нет ответа, сервер недоступен, столкнулись два одинаковых запроса — повторяем', () => {
    expect(isRetryable(new ApiError(NO_RESPONSE, 'таймаут'))).toBe(true);
    expect(isRetryable(new ApiError(503, 'недоступен'))).toBe(true);
    expect(isRetryable(new ApiError(409, 'конфликт'))).toBe(true);
  });

  it('ошибка в данных или нет доступа — повтор не поможет', () => {
    expect(isRetryable(new ApiError(400, 'вес'))).toBe(false);
    expect(isRetryable(new ApiError(404, 'нет тренировки'))).toBe(false);
    expect(isRetryable(new Error('что-то ещё'))).toBe(false);
  });

  it('id подхода — UUID, каждый раз новый', () => {
    const a = newClientId();
    expect(a).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    expect(newClientId()).not.toBe(a);
  });
});
