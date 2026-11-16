import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { workoutsApi, type CatalogExercise, type WorkoutTemplate } from '../api/workouts';
import { ExerciseImage } from '../components/ExerciseImage';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { TextField } from '../components/ui/TextField';
import { AddExerciseDialog } from '../components/workout/AddExerciseDialog';
import { ConfirmDialog } from '../components/workout/ConfirmDialog';
import { CURRENT_WORKOUT_KEY } from '../hooks/useWorkout';
import { TEMPLATE_COLORS, move } from '../lib/templates';
import { exercisesLabel } from './StartWorkoutPage';
import styles from './TemplatePage.module.css';

const COLOR_NAMES: Record<string, string> = {
  orange: 'Оранжевый',
  blue: 'Синий',
  green: 'Зелёный',
  violet: 'Фиолетовый',
};

/**
 * Комплекс: посмотреть и поменять. Открывается значком ✎ на плитке («Новая тренировка»);
 * нажатие на саму плитку, как и раньше, сразу готовит тренировку.
 * Свой — правим название, цвет, упражнения и порядок. Готовый — только смотреть и «Скопировать в мои».
 * День программы — только смотреть: его упражнения меняются в программе (иначе программа и комплекс разъедутся).
 */
export function TemplatePage() {
  const { templateId } = useParams();
  // список уже в кэше со страницы «Новая тренировка»; в нём же признак «день программы»
  const templates = useQuery({ queryKey: ['templates'], queryFn: workoutsApi.templates });
  const template = templates.data?.find((t) => t.id === templateId);

  if (templates.isPending) {
    return <p className={styles.muted}>Загружаем…</p>;
  }
  if (!template) {
    return (
      <div className={styles.page}>
        <PageHeader title="Комплекс не найден" back={{ to: '/workout', label: 'Новая тренировка' }} />
        <p className={styles.muted}>Возможно, его уже удалили.</p>
      </div>
    );
  }
  // key: открыли другой комплекс — форма собирается заново со своими начальными значениями
  return <TemplateView key={template.id} template={template} />;
}

function TemplateView({ template }: { template: WorkoutTemplate }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const editable = template.custom && !template.program;

  const [name, setName] = useState(template.name);
  const initialColor = template.color && TEMPLATE_COLORS[template.color] ? template.color : 'blue';
  const [color, setColor] = useState(initialColor);
  const [exercises, setExercises] = useState(template.exercises.map((e) => ({ id: e.id, name: e.name })));
  const [adding, setAdding] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // картинки упражнений — из каталога (тот же кэш, что у окна «Добавить упражнение»)
  const catalog = useQuery({ queryKey: ['exercises', ''], queryFn: () => workoutsApi.exercises('') });
  const imageOf = (id: string) => catalog.data?.find((e) => e.id === id)?.imageKey ?? null;

  const dirty =
    name.trim() !== template.name ||
    color !== initialColor ||
    exercises.map((e) => e.id).join() !== template.exercises.map((e) => e.id).join();
  const valid = name.trim().length > 0 && exercises.length > 0;

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['templates'] });
    // название комплекса видно в плане недели и на главной
    void queryClient.invalidateQueries({ queryKey: ['plan'] });
  };

  const save = useMutation({
    mutationFn: () =>
      workoutsApi.updateTemplate(template.id, { name: name.trim(), color, exerciseIds: exercises.map((e) => e.id) }),
    onSuccess: refresh,
  });
  const remove = useMutation({
    mutationFn: () => workoutsApi.deleteTemplate(template.id),
    onSuccess: () => {
      refresh();
      navigate('/workout', { replace: true });
    },
  });
  const copy = useMutation({
    mutationFn: () => workoutsApi.copyTemplate(template.id),
    onSuccess: (created) => {
      refresh();
      navigate(`/templates/${created.id}`, { replace: true });
    },
  });
  const start = useMutation({
    mutationFn: () => workoutsApi.start(template.id),
    onSuccess: (workout) => {
      queryClient.setQueryData(CURRENT_WORKOUT_KEY, workout);
      navigate('/workout/active');
    },
    onError: (error) => {
      // 409: тренировка уже идёт — туда и ведём
      if (error instanceof ApiError && error.status === 409) {
        navigate('/workout/active');
      }
    },
  });

  const addExercise = async (id: string) => {
    // только что созданного своего упражнения в кэше каталога ещё может не быть — тогда перечитаем
    let exercise: CatalogExercise | undefined = catalog.data?.find((e) => e.id === id);
    if (!exercise) {
      const fresh = await queryClient.fetchQuery({
        queryKey: ['exercises', ''],
        queryFn: () => workoutsApi.exercises(''),
        staleTime: 0,
      });
      exercise = fresh.find((e) => e.id === id);
    }
    if (exercise) {
      const { id: exerciseId, name: exerciseName } = exercise;
      setExercises((list) => [...list, { id: exerciseId, name: exerciseName }]);
    }
    setAdding(false);
  };

  return (
    <div className={styles.page}>
      <PageHeader
        eyebrow={template.program ? 'День программы' : template.custom ? 'Мой комплекс' : 'Готовый комплекс'}
        title={editable ? name.trim() || 'Без названия' : template.name}
        subtitle={exercisesLabel(exercises.length)}
        back={{ to: '/workout', label: 'Новая тренировка' }}
      />

      {template.program && (
        <Card className={styles.notice}>
          <span>Упражнения этого дня меняются в программе — кнопкой ⇄ у упражнения.</span>
          <Link to="/program">К программе →</Link>
        </Card>
      )}
      {!template.custom && (
        <Card className={styles.notice}>
          <span>Готовый комплекс нельзя менять. Скопируйте его в «Мои комплексы» — и правьте как хотите.</span>
        </Card>
      )}

      {editable && (
        <Card as="section" className={styles.form}>
          <TextField label="Название" maxLength={100} required value={name} onChange={(e) => setName(e.target.value)} />
          <fieldset className={styles.colors}>
            <legend className={styles.legend}>Цвет метки</legend>
            {Object.entries(TEMPLATE_COLORS).map(([key, hex]) => (
              <label key={key} className={styles.color} title={COLOR_NAMES[key]}>
                <input
                  type="radio"
                  name="color"
                  value={key}
                  checked={color === key}
                  onChange={() => setColor(key)}
                  aria-label={COLOR_NAMES[key]}
                />
                <span style={{ background: hex }} aria-hidden="true" />
              </label>
            ))}
          </fieldset>
        </Card>
      )}

      <section className={styles.section} aria-labelledby="exercises">
        <SectionLabel id="exercises">Упражнения</SectionLabel>
        <ol className={styles.list}>
          {exercises.map((exercise, index) => (
            <li key={exercise.id} className={styles.row}>
              <span className={styles.number}>{index + 1}</span>
              <ExerciseImage imageKey={imageOf(exercise.id)} />
              <Link to={`/exercises/${exercise.id}`} className={styles.name}>
                {exercise.name}
              </Link>
              {editable && (
                <span className={styles.tools}>
                  <button
                    type="button"
                    className={styles.tool}
                    aria-label={`Выше: ${exercise.name}`}
                    disabled={index === 0}
                    onClick={() => setExercises((list) => move(list, index, -1))}
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    className={styles.tool}
                    aria-label={`Ниже: ${exercise.name}`}
                    disabled={index === exercises.length - 1}
                    onClick={() => setExercises((list) => move(list, index, 1))}
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    className={styles.tool}
                    aria-label={`Убрать: ${exercise.name}`}
                    onClick={() => setExercises((list) => list.filter((e) => e.id !== exercise.id))}
                  >
                    ✕
                  </button>
                </span>
              )}
            </li>
          ))}
          {editable && (
            <li>
              <button type="button" className={styles.addRow} onClick={() => setAdding(true)}>
                + Добавить упражнение
              </button>
            </li>
          )}
        </ol>
        {editable && exercises.length === 0 && <p className={styles.muted}>Добавьте хотя бы одно упражнение.</p>}
      </section>

      <div className={styles.actions}>
        {editable && dirty ? (
          <>
            {save.isError && (
              <p className={styles.error} role="alert">
                {save.error.message}
              </p>
            )}
            <Button fullWidth disabled={!valid || save.isPending} onClick={() => save.mutate()}>
              {save.isPending ? 'Сохраняем…' : 'Сохранить'}
            </Button>
            <Button
              variant="secondary"
              fullWidth
              onClick={() => {
                setName(template.name);
                setColor(initialColor);
                setExercises(template.exercises.map((e) => ({ id: e.id, name: e.name })));
              }}
            >
              Отменить изменения
            </Button>
          </>
        ) : (
          <>
            {save.isSuccess && <p className={styles.saved}>Сохранено ✓</p>}
            <Button fullWidth disabled={start.isPending} onClick={() => start.mutate()}>
              Начать тренировку
            </Button>
            {!template.custom && (
              <Button variant="secondary" fullWidth disabled={copy.isPending} onClick={() => copy.mutate()}>
                Скопировать в мои
              </Button>
            )}
            {copy.isError && <p className={styles.error}>{copy.error.message}</p>}
            {start.isError && !(start.error instanceof ApiError && start.error.status === 409) && (
              <p className={styles.error}>{start.error.message}</p>
            )}
          </>
        )}
        {editable && (
          <Button variant="surface" fullWidth onClick={() => setDeleting(true)}>
            Удалить комплекс
          </Button>
        )}
        {template.program && (
          <ButtonLink to="/program" variant="secondary" fullWidth>
            Открыть программу
          </ButtonLink>
        )}
      </div>

      {adding && (
        <AddExerciseDialog
          target="template"
          existingIds={exercises.map((e) => e.id)}
          adding={false}
          onAdd={(id) => void addExercise(id)}
          onClose={() => setAdding(false)}
        />
      )}

      {deleting && (
        <ConfirmDialog
          title="Удалить комплекс?"
          text={`«${template.name}» исчезнет из «Мои комплексы» и из плана недели. Прошлые тренировки по нему останутся в истории.`}
          confirmLabel="Удалить"
          busy={remove.isPending}
          onConfirm={() => remove.mutate()}
          onClose={() => setDeleting(false)}
        />
      )}
    </div>
  );
}
