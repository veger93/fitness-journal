import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { programsApi, type ProgramExclusion } from '../../api/programs';
import { formatShortDate } from '../../lib/format';
import { GOALS, programWeek } from '../../lib/program';
import { ListRow, ListRowGroup } from '../ui/ListRow';
import { SectionLabel } from '../ui/SectionLabel';
import styles from './Profile.module.css';

/**
 * Раздел «Программа тренировок» в профиле (макет E2) — единственный вход в анкету после онбординга:
 * на главной строки нет (решение 13.11). Нет программы — одна строка «Подобрать программу».
 */
export function ProgramCard() {
  const program = useQuery({ queryKey: ['program'], queryFn: programsApi.current });
  const exclusions = useQuery({ queryKey: ['program', 'exclusions'], queryFn: programsApi.exclusions });
  const queryClient = useQueryClient();
  const restore = useMutation({
    mutationFn: programsApi.removeExclusion,
    // убираем строку сразу по ответу, без повторного запроса списка
    onSuccess: (_, exerciseId) =>
      queryClient.setQueryData(['program', 'exclusions'], (list: ProgramExclusion[] | undefined) =>
        list?.filter((e) => e.exerciseId !== exerciseId),
      ),
  });

  return (
    <section className={styles.programSection} aria-labelledby="program-title">
      <SectionLabel id="program-title">Программа тренировок</SectionLabel>
      {program.isPending ? (
        <p className={styles.muted}>Загружаем…</p>
      ) : program.data ? (
        <ListRowGroup>
          <ListRow
            to="/program"
            mark={`${program.data.daysPerWeek}×`}
            tone="inverse"
            title={program.data.title}
            text={`${GOALS.find((g) => g.value === program.data!.goal)?.title} · ${
              program.data.reviewDue ? 'пора пересмотреть' : `неделя ${programWeek(program.data.startedAt)} из 6`
            }`}
          />
          <ListRow
            to="/program/new"
            mark="↻"
            title="Пройти анкету заново"
            text={`пересмотр — ${formatShortDate(`${program.data.reviewAt}T12:00:00`)}`}
          />
        </ListRowGroup>
      ) : (
        <ListRowGroup>
          <ListRow
            to="/program/new"
            mark="+"
            tone="accent"
            title="Подобрать программу"
            text="4 коротких шага, около минуты"
          />
        </ListRowGroup>
      )}

      {/* «больше не предлагать» — живёт отдельно от программы: действует и на следующую анкету */}
      {exclusions.data && exclusions.data.length > 0 && (
        <ListRowGroup label="Не предлагать">
          {exclusions.data.map((e) => (
            <ListRow
              key={e.exerciseId}
              mark="–"
              title="Не предлагать"
              text={e.exerciseName.toLowerCase()}
              value={
                <button
                  type="button"
                  className={styles.linkButton}
                  disabled={restore.isPending}
                  onClick={() => restore.mutate(e.exerciseId)}
                  aria-label={`Вернуть в подбор: ${e.exerciseName}`}
                >
                  Вернуть
                </button>
              }
            />
          ))}
        </ListRowGroup>
      )}
      {restore.isError && <p className={styles.error}>{restore.error.message}</p>}
    </section>
  );
}
