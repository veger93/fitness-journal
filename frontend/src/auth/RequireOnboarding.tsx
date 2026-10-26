import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { usersApi } from '../api/users';

/**
 * Пока физданные не заполнены — отправляем на онбординг.
 * Если профиль не загрузился (сервис недоступен), приложение не блокируем: онбординг подождёт до следующего входа.
 */
export function RequireOnboarding({ children }: { children: ReactNode }) {
  const profile = useQuery({ queryKey: ['profile'], queryFn: usersApi.profile });
  if (profile.isPending) {
    return null;
  }
  if (profile.data && !profile.data.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />;
  }
  return children;
}
