import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './components/AppLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { ActiveWorkoutPage } from './pages/ActiveWorkoutPage';
import { ProfilePage } from './pages/ProfilePage';
import { RegisterPage } from './pages/RegisterPage';
import { StartWorkoutPage } from './pages/StartWorkoutPage';
import { WorkoutSummaryPage } from './pages/WorkoutSummaryPage';
import { ThemeProvider } from './theme/ThemeProvider';

// Один клиент на всё приложение: в нём кэш ответов сервера.
// staleTime 30 с: данные свежие полминуты — переходы между страницами не дёргают сервер заново.
const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
});

export function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                element={
                  <RequireAuth>
                    <AppLayout />
                  </RequireAuth>
                }
              >
                <Route path="/" element={<HomePage />} />
                <Route path="/workout" element={<StartWorkoutPage />} />
                <Route path="/workout/active" element={<ActiveWorkoutPage />} />
                <Route path="/workout/:workoutId/summary" element={<WorkoutSummaryPage />} />
                <Route path="/profile" element={<ProfilePage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
