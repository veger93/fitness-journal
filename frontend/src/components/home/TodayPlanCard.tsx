import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { workoutsApi, type WeeklyPlan, type Workout } from '../../api/workouts';
import { CURRENT_WORKOUT_KEY } from '../../hooks/useWorkout';
import { DAYS, completedToday, nextPlanned, todayOf } from '../../lib/plan';
import { exercisesLabel } from '../../pages/StartWorkoutPage';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import styles from './Home.module.css';

/**
 * "Сегодня по плану" на главной. Случаи: сегодня тренировка — кнопка "Начать", а если уже сделана — "выполнено";
 * сегодня отдых — когда следующая; плана нет — приглашение его составить.
 */
export function TodayPlanCard({ plan, recent }: { plan: WeeklyPlan; recent: Workout[] }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const today = todayOf();
  const todayTemplate = plan.days.find((d) => d.dayOfWeek === today)?.template ?? null;
  const next = nextPlanned(plan, today);

  const start = useMutation({
    mutationFn: (templateId: string) => workoutsApi.start(templateId),
    onSuccess: (workout) => {
      queryClient.setQueryData(CURRENT_WORKOUT_KEY, workout);
      navigate('/workout/active');
    },
  });

  const nextText = next
    ? `Следующая тренировка — ${DAYS.find((d) => d.day === next.day)?.name.toLowerCase()}: ${next.template.name}.`
    : null;

  if (todayTemplate && completedToday(recent, todayTemplate.id)) {
    return (
      <Card as="section">
        <span className={styles.signalLabel}>Сегодня по плану</span>
        <h2 className={styles.planTitle}>{todayTemplate.name} — выполнено ✓</h2>
        <p className={styles.signalText}>Отличная работа! {nextText}</p>
        <Link to="/plan" className={styles.planLink}>
          План на неделю
        </Link>
      </Card>
    );
  }

  if (todayTemplate) {
    return (
      <Card variant="inverse" size="section" as="section">
        <span className={styles.signalLabel}>Сегодня по плану</span>
        <h2 className={styles.planTitle}>{todayTemplate.name}</h2>
        <p className={styles.signalText}>{exercisesLabel(todayTemplate.exercises.length)}</p>
        <div className={styles.planActions}>
          <Button variant="surface" onClick={() => start.mutate(todayTemplate.id)} disabled={start.isPending}>
            Начать
          </Button>
          <Link to="/plan" className={styles.planLink}>
            План на неделю
          </Link>
        </div>
        {start.isError && <p className={styles.signalText}>{start.error.message}</p>}
      </Card>
    );
  }

  if (next) {
    return (
      <Card as="section">
        <span className={styles.signalLabel}>Сегодня по плану</span>
        <h2 className={styles.planTitle}>День отдыха</h2>
        <p className={styles.signalText}>{nextText}</p>
        <Link to="/plan" className={styles.planLink}>
          План на неделю
        </Link>
      </Card>
    );
  }

  return (
    <Card as="section">
      <h2 className={styles.planTitle}>Составьте план на неделю</h2>
      <p className={styles.signalText}>
        Выберите комплексы по дням — здесь будет видно, что сегодня по плану, и можно начать одной кнопкой.
      </p>
      <Link to="/plan" className={styles.planLink}>
        Составить план →
      </Link>
    </Card>
  );
}
