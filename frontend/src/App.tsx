import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './components/AppLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { ActiveWorkoutPage } from './pages/ActiveWorkoutPage';
import { ExercisePage } from './pages/ExercisePage';
import { OnboardingPage } from './pages/OnboardingPage';
import { ReportPage } from './pages/ReportPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { RequireOnboarding } from './auth/RequireOnboarding';
import { ExercisesPage } from './pages/ExercisesPage';
import { HistoryPage } from './pages/HistoryPage';
import { PlanPage } from './pages/PlanPage';
import { ProfilePage } from './pages/ProfilePage';
import { RecordsPage } from './pages/RecordsPage';
import { RunningPage } from './pages/RunningPage';
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
                path="/onboarding"
                element={
                  <RequireAuth>
                    <OnboardingPage />
                  </RequireAuth>
                }
              />
              <Route
                element={
                  <RequireAuth>
                    <RequireOnboarding>
                      <AppLayout />
                    </RequireOnboarding>
                  </RequireAuth>
                }
              >
                <Route path="/" element={<HomePage />} />
                <Route path="/workout" element={<StartWorkoutPage />} />
                <Route path="/workout/active" element={<ActiveWorkoutPage />} />
                <Route path="/workout/:workoutId/summary" element={<WorkoutSummaryPage />} />
                <Route path="/exercises" element={<ExercisesPage />} />
                <Route path="/exercises/:exerciseId" element={<ExercisePage />} />
                <Route path="/records" element={<RecordsPage />} />
                <Route path="/running" element={<RunningPage />} />
                <Route path="/plan" element={<PlanPage />} />
                <Route path="/history" element={<HistoryPage />} />
                <Route path="/history/:workoutId" element={<WorkoutSummaryPage mode="view" />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/notifications" element={<NotificationsPage />} />
                <Route path="/reports" element={<ReportPage />} />
                <Route path="/reports/:month" element={<ReportPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
