import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './components/AppLayout';
import { HomePage } from './pages/HomePage';
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/LoginPage';
import { ClaimPage } from './pages/ClaimPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
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
import { ActingClientProvider } from './trainer/ActingClientProvider';

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
          {/* режим тренера: за какого клиента сейчас работаем (заголовок X-Client-Id в запросах) */}
          <ActingClientProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                {/* передача аккаунта клиенту по ссылке тренера — без входа */}
                <Route path="/claim/:token" element={<ClaimPage />} />
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
                    // гость на "/" видит страницу о приложении, на остальных закрытых страницах — вход
                    <RequireAuth guestHome={<LandingPage />}>
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
          </ActingClientProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
