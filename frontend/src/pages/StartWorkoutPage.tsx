import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Navigate, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { workoutsApi, type WorkoutTemplate } from '../api/workouts';
import { Button } from '../components/ui/Button';
import { CURRENT_WORKOUT_KEY, useCurrentWorkout } from '../hooks/useWorkout';
import styles from './WorkoutPages.module.css';

/** "Начать тренировку": выбрать комплекс или пустую. Если тренировка уже идёт — сразу к ней. */
export function StartWorkoutPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const current = useCurrentWorkout();
  const templates = useQuery({ queryKey: ['templates'], queryFn: workoutsApi.templates });

  const start = useMutation({
    mutationFn: workoutsApi.start,
    onSuccess: (workout) => {
      queryClient.setQueryData(CURRENT_WORKOUT_KEY, workout);
      navigate('/workout/active');
    },
    onError: (error) => {
      // 409: тренировка уже идёт (например, начата с другого устройства) — просто перечитаем
      if (error instanceof ApiError && error.status === 409) {
        queryClient.invalidateQueries({ queryKey: CURRENT_WORKOUT_KEY });
      }
    },
  });

  if (current.data) {
    return <Navigate to="/workout/active" replace />;
  }

  const mine = templates.data?.filter((t) => t.custom) ?? [];
  const ready = templates.data?.filter((t) => !t.custom) ?? [];

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Новая тренировка</h1>
      <p className={styles.muted}>
        Выберите комплекс или соберите свою тренировку. Время пойдёт, только когда начнёте — можно подготовиться
        заранее.
      </p>
      <Button fullWidth onClick={() => start.mutate(null)} disabled={start.isPending}>
        Своя тренировка
      </Button>
      {start.isError && <p className={styles.muted}>{start.error.message}</p>}

      {mine.length > 0 && (
        <TemplateGroup
          title="Мои комплексы"
          templates={mine}
          disabled={start.isPending}
          onPick={(id) => start.mutate(id)}
        />
      )}
      {ready.length > 0 && (
        <TemplateGroup
          title="Готовые комплексы"
          templates={ready}
          disabled={start.isPending}
          onPick={(id) => start.mutate(id)}
        />
      )}
      {templates.isPending && <p className={styles.muted}>Загружаем комплексы…</p>}
    </div>
  );
}

function TemplateGroup({
  title,
  templates,
  disabled,
  onPick,
}: {
  title: string;
  templates: WorkoutTemplate[];
  disabled: boolean;
  onPick: (id: string) => void;
}) {
  return (
    <section className={styles.page}>
      <h2 className={styles.sectionTitle}>{title}</h2>
      <div className={styles.templates}>
        {templates.map((template) => (
          <button
            key={template.id}
            type="button"
            className={styles.template}
            disabled={disabled}
            onClick={() => onPick(template.id)}
          >
            <span className={styles.templateName}>{template.name}</span>
            <span className={styles.muted}>{exercisesLabel(template.exercises.length)}</span>
          </button>
        ))}
      </div>
    </section>
  );
}

/** 1 упражнение, 2 упражнения, 5 упражнений — склонение по-русски */
export function exercisesLabel(count: number): string {
  const mod10 = count % 10;
  const mod100 = count % 100;
  const word =
    mod10 === 1 && mod100 !== 11
      ? 'упражнение'
      : mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)
        ? 'упражнения'
        : 'упражнений';
  return `${count} ${word}`;
}
