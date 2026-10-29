import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RequireAuth } from './RequireAuth';

// подменяем useAuth: тестируем только решение "кого куда пустить"
const auth = { isAuthenticated: false };
vi.mock('./AuthProvider', () => ({ useAuth: () => auth }));

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<p>форма входа</p>} />
        <Route element={<RequireAuth guestHome={<p>лендинг</p>}>закрытая страница</RequireAuth>}>
          <Route path="/" element={null} />
          <Route path="/history" element={null} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RequireAuth', () => {
  afterEach(() => {
    auth.isAuthenticated = false;
  });

  it('гость на главной видит лендинг, а не форму входа', () => {
    renderAt('/');
    expect(screen.getByText('лендинг')).toBeTruthy();
  });

  it('гость на закрытой странице уходит на вход', () => {
    renderAt('/history');
    expect(screen.getByText('форма входа')).toBeTruthy();
  });

  it('вошедший на главной видит приложение', () => {
    auth.isAuthenticated = true;
    renderAt('/');
    expect(screen.getByText('закрытая страница')).toBeTruthy();
  });
});
