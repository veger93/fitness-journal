import { useQuery } from '@tanstack/react-query';
import { programsApi } from '../../api/programs';
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
    </section>
  );
}
