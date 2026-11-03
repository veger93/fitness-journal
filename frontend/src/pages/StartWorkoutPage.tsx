import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { workoutsApi, type WorkoutTemplate } from '../api/workouts';
import { TodayPlanCard } from '../components/home/TodayPlanCard';
import { ConfirmDialog } from '../components/workout/ConfirmDialog';
import { CURRENT_WORKOUT_KEY, useCurrentWorkout } from '../hooks/useWorkout';
import { completedToday, todayOf } from '../lib/plan';
import styles from './WorkoutPages.module.css';

/**
 * "Начать тренировку": выбрать комплекс или пустую. Если тренировка уже идёт — сразу к ней.
 * Сверху — то, что нажимают чаще всего: "сегодня по плану", "своя" и "повторить последнюю";
 * ниже комплексы компактными плитками 2 в ряд (на телефоне раньше каждый был карточкой во всю ширину).
 */
export function StartWorkoutPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const current = useCurrentWorkout();
  const templates = useQuery({ queryKey: ['templates'], queryFn: workoutsApi.templates });
  const plan = useQuery({ queryKey: ['plan'], queryFn: workoutsApi.plan });
  // тот же ключ и запрос, что на главной: последняя тренировка для "повторить" и "сегодня уже сделано"
  const recent = useQuery({ queryKey: ['history', 'recent'], queryFn: () => workoutsApi.journal(0, 5) });
  const last = recent.data?.content[0] ?? null;
  const todayPlanned = plan.data?.days.find((d) => d.dayOfWeek === todayOf())?.template ?? null;
  const plannedPending = todayPlanned !== null && !completedToday(recent.data?.content ?? [], todayPlanned.id);

  const [deleting, setDeleting] = useState<WorkoutTemplate | null>(null);
  const remove = useMutation({
    mutationFn: (id: string) => workoutsApi.deleteTemplate(id),
    onSuccess: () => {
      setDeleting(null);
      queryClient.invalidateQueries({ queryKey: ['templates'] });
      queryClient.invalidateQueries({ queryKey: ['plan'] }); // день плана с этим комплексом удалился вместе с ним
    },
  });

  const start = useMutation({
    // templateId — комплекс (null — пустая тренировка); repeatOf — повторить прошлую тренировку
    mutationFn: ({ templateId, repeatOf }: { templateId?: string | null; repeatOf?: string }) =>
      repeatOf ? workoutsApi.repeat(repeatOf) : workoutsApi.start(templateId ?? null),
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
      <h1 className={`${styles.title} ${styles.startTitle}`}>Новая тренировка</h1>
      <p className={`${styles.muted} ${styles.intro}`}>
        Выберите комплекс или соберите свою тренировку. Время пойдёт, только когда начнёте — можно подготовиться
        заранее.
      </p>

      {plan.data && plannedPending && <TodayPlanCard plan={plan.data} recent={recent.data?.content ?? []} />}

      <div className={styles.quick}>
        <button
          type="button"
          className={styles.quickAction}
          disabled={start.isPending}
          onClick={() => start.mutate({ templateId: null })}
        >
          <span className={styles.quickTitle}>＋ Своя тренировка</span>
          <span className={styles.muted}>любые упражнения</span>
        </button>
        {last && (
          <button
            type="button"
            className={styles.quickAction}
            disabled={start.isPending}
            onClick={() => start.mutate({ repeatOf: last.id })}
          >
            <span className={styles.quickTitle}>↻ Повторить последнюю</span>
            <span className={`${styles.muted} ${styles.ellipsis}`}>{last.name || 'Тренировка'}</span>
          </button>
        )}
      </div>
      {start.isError && <p className={styles.muted}>{start.error.message}</p>}

      {mine.length > 0 && (
        <TemplateGroup
          onDelete={setDeleting}
          title="Мои комплексы"
          templates={mine}
          disabled={start.isPending}
          onPick={(id) => start.mutate({ templateId: id })}
        />
      )}
      {ready.length > 0 && (
        <TemplateGroup
          title="Готовые комплексы"
          templates={ready}
          disabled={start.isPending}
          onPick={(id) => start.mutate({ templateId: id })}
        />
      )}
      {templates.isPending && <p className={styles.muted}>Загружаем комплексы…</p>}

      <Link to="/plan" className={styles.planLink}>
        План на неделю →
      </Link>

      {deleting && (
        <ConfirmDialog
          title="Удалить комплекс?"
          text={`«${deleting.name}» исчезнет из «Мои комплексы». Прошлые тренировки по нему останутся в истории.`}
          confirmLabel="Удалить"
          busy={remove.isPending}
          onConfirm={() => remove.mutate(deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}
    </div>
  );
}

function TemplateGroup({
  title,
  templates,
  disabled,
  onPick,
  onDelete,
}: {
  title: string;
  templates: WorkoutTemplate[];
  disabled: boolean;
  onPick: (id: string) => void;
  /** только для своих комплексов — готовые удалить нельзя */
  onDelete?: (template: WorkoutTemplate) => void;
}) {
  return (
    <section className={styles.group}>
      <h2 className={styles.groupTitle}>{title}</h2>
      <div className={styles.templates}>
        {templates.map((template) => (
          // две отдельные кнопки рядом, а не кнопка в кнопке: вложенные <button> в HTML запрещены
          <div key={template.id} className={`${styles.templateWrap} ${onDelete ? styles.withDelete : ''}`}>
            <button type="button" className={styles.template} disabled={disabled} onClick={() => onPick(template.id)}>
              <span
                className={styles.templateMark}
                style={{ background: templateColor(template) }}
                aria-hidden="true"
              />
              <span className={styles.templateName}>{template.name}</span>
              <span className={styles.muted}>{exercisesLabel(template.exercises.length)}</span>
            </button>
            {onDelete && (
              <button
                type="button"
                className={styles.templateDelete}
                aria-label={`Удалить комплекс ${template.name}`}
                onClick={() => onDelete(template)}
              >
                <svg
                  aria-hidden="true"
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                >
                  <path d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

/** Цвета меток комплексов — читаются и на светлой, и на тёмной карточке */
const TEMPLATE_COLORS: Record<string, string> = {
  violet: '#7c3aed',
  green: '#16a34a',
  blue: '#2563eb',
  orange: '#ea580c',
};
const PALETTE = Object.values(TEMPLATE_COLORS);

/** Свой цвет у комплекса (у готовых он есть); нет — стабильный цвет по id, чтобы метка не "прыгала" */
function templateColor(template: WorkoutTemplate): string {
  if (template.color && TEMPLATE_COLORS[template.color]) {
    return TEMPLATE_COLORS[template.color];
  }
  let hash = 0;
  for (const char of template.id) {
    hash = (hash * 31 + char.charCodeAt(0)) | 0;
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
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
