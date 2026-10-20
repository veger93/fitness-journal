import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useCarousel } from './useCarousel';

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('useCarousel', () => {
  it('листает сам каждые intervalMs', () => {
    const { result } = renderHook(() => useCarousel(3, { intervalMs: 1000 }));
    act(() => vi.advanceTimersByTime(2000));
    expect(result.current.position).toBe(2);
    expect(result.current.active).toBe(2);
  });

  it('после копии первого слайда возвращается в начало без анимации', () => {
    const { result } = renderHook(() => useCarousel(2, { intervalMs: 1000 }));
    act(() => vi.advanceTimersByTime(2000)); // 0 -> 1 -> 2 (копия первого)
    expect(result.current.active).toBe(0);
    act(() => result.current.onTransitionEnd());
    expect(result.current.position).toBe(0);
    expect(result.current.animate).toBe(false);
  });

  it('на паузе стоит', () => {
    const { result } = renderHook(() => useCarousel(3, { intervalMs: 1000, paused: true }));
    act(() => vi.advanceTimersByTime(5000));
    expect(result.current.position).toBe(0);
  });

  it('назад с первого — на последний', () => {
    const { result } = renderHook(() => useCarousel(4, { paused: true }));
    act(() => result.current.prev());
    expect(result.current.active).toBe(3);
  });
});
