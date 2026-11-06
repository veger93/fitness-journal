import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { workoutsApi, type DayOfWeek, type WorkoutTemplate } from '../api/workouts';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { Select } from '../components/ui/Select';
import { plural } from '../lib/format';
import { BODY_PART_NAMES, DAYS, bodyPartFrequency, planHints, todayOf } from '../lib/plan';
import styles from './PlanPage.module.css';

type Draft = Record<DayOfWeek, string>; // день -> id комплекса ('' — отдых)

/**
 * План на неделю: какой комплекс в какой день. Неделя повторяется — так видно прогресс
 * (одни и те же упражнения, растут веса). Подсказки снизу — по исследованиям, мягко.
 */
export function PlanPage() {
  const queryClient = useQueryClient();
  const plan = useQuery({ queryKey: ['plan'], queryFn: workoutsApi.plan });
  const templates = useQuery({ queryKey: ['templates'], queryFn: workoutsApi.templates });
  const [draft, setDraft] = useState<Draft | null>(null);

  // черновик заполняем из сохранённого плана один раз, дальше человек редактирует его
  useEffect(() => {
    if (plan.data && draft === null) {
      setDraft(Object.fromEntries(plan.data.days.map((d) => [d.dayOfWeek, d.template?.id ?? ''])) as Draft);
    }
  }, [plan.data, draft]);

  const save = useMutation({
    mutationFn: (d: Draft) =>
      workoutsApi.savePlan(DAYS.map(({ day }) => ({ dayOfWeek: day, templateId: d[day] || null }))),
    onSuccess: (saved) => queryClient.setQueryData(['plan'], saved),
  });

  if (!draft || !templates.data) {
    return <p className={styles.muted}>{plan.isError ? plan.error.message : 'Загружаем…'}</p>;
  }

  const byId = new Map(templates.data.map((t) => [t.id, t]));
  const chosen: (WorkoutTemplate | null)[] = DAYS.map(({ day }) => byId.get(draft[day]) ?? null);
  const workouts = chosen.filter(Boolean).length;
  const frequency = [...bodyPartFrequency(chosen)];
  const hints = planHints(chosen);
  const mine = templates.data.filter((t) => t.custom);
  const ready = templates.data.filter((t) => !t.custom);
  const today = todayOf();

  const saveButton = (
    <Button onClick={() => save.mutate(draft)} disabled={save.isPending}>
      {save.isPending ? 'Сохраняем…' : save.isSuccess ? 'Сохранено ✓' : 'Сохранить план'}
    </Button>
  );

  return (
    <div className={styles.page}>
      <PageHeader
        title="План на неделю"
        subtitle="Выберите комплекс на каждый тренировочный день — неделя будет повторяться."
      />

      {/* ПК: дни (2/3) | «Неделя целиком» и подсказки (1/3). Телефон: дни → сохранить → неделя целиком */}
      <PageColumns
        aside={
          <>
            <section className={styles.section}>
              <SectionLabel>Неделя целиком</SectionLabel>
              <Card>
                <p className={styles.plain}>
                  <strong>{plural(workouts, 'тренировка', 'тренировки', 'тренировок')}</strong> в неделю
                </p>
                {frequency.length > 0 && (
                  <p className={styles.muted}>
                    {frequency.map(([part, count]) => `${BODY_PART_NAMES[part]} ×${count}`).join(' · ')}
                  </p>
                )}
                {hints.map((hint) => (
                  <p key={hint} className={styles.hint}>
                    {hint}*
                  </p>
                ))}
              </Card>
            </section>
            {hints.length > 0 && (
              <p className="footnote">
                * По обзорам исследований: группу мышц выгоднее тренировать 2 раза в неделю, но главное — общий объём за
                неделю; большинству хватает 3–5 тренировок в неделю.
              </p>
            )}
          </>
        }
      >
        <Card as="section">
          <div className={styles.days}>
            {DAYS.map(({ day, name }) => (
              <Select
                key={day}
                row
                inset
                label={name}
                labelExtra={day === today && <Badge variant="inverse">сегодня</Badge>}
                value={draft[day]}
                onChange={(value) => {
                  save.reset();
                  setDraft({ ...draft, [day]: value });
                }}
              >
                <option value="">Отдых</option>
                {mine.length > 0 && (
                  <optgroup label="Мои комплексы">
                    {mine.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                  </optgroup>
                )}
                <optgroup label="Готовые комплексы">
                  {ready.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </optgroup>
              </Select>
            ))}
          </div>
        </Card>

        {save.isError && (
          <p className={styles.error} role="alert">
            {save.error.message}
          </p>
        )}
        <div className={styles.actions}>{saveButton}</div>
      </PageColumns>
    </div>
  );
}
