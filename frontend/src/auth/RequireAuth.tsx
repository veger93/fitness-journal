import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthProvider';

interface RequireAuthProps {
  children: ReactNode;
  /** что показать гостю на главной "/" вместо экрана входа (лендинг) */
  guestHome?: ReactNode;
}

/**
 * Закрытые страницы: без входа — на /login, а после входа вернём туда, куда шли.
 * Исключение — главная: по ссылке vegas-fit.ru гость видит страницу о приложении, а не форму входа.
 */
export function RequireAuth({ children, guestHome }: RequireAuthProps) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    if (guestHome && location.pathname === '/') {
      return guestHome;
    }
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}
