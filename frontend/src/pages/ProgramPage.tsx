import { useQuery } from '@tanstack/react-query';
import { Link, useLocation } from 'react-router-dom';
import { programsApi, type Program, type ProgramDay } from '../api/programs';
import { Badge } from '../components/ui/Badge';
import { ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ListRow, ListRowGroup } from '../components/ui/ListRow';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { formatShortDate } from '../lib/format';
import {
  dayPreview,
  headline,
  programReasons,
  programSummary,
  programWeek,
  restDaysText,
  slotScheme,
  weekDaysText,
} from '../lib/program';
import { useActingClient } from '../trainer/ActingClientProvider';
import type { ProgramCreatedState } from './ProgramQuizPage';
import styles from './ProgramPage.module.css';

/**
 * «Ваша программа» (макет R1 + дни из R2): сводка, неделя по дням с упражнениями, пояснения генератора, «почему так».
 * Сразу после анкеты (state.created) — «Готово» и кнопка «Начать программу»; потом сюда приходят из профиля.
 */
export function ProgramPage() {
  const { client: acting } = useActingClient();
  const created = (useLocation().state as ProgramCreatedState | null)?.created === true;
  const consultDoctor = (useLocation().state as ProgramCreatedState | null)?.consultDoctor === true;
  const program = useQuery({ queryKey: ['program'], queryFn: programsApi.current });

  if (program.isPending) {
    return <p className={styles.muted}>Загружаем…</p>;
  }
  if (program.isError) {
    return <p className={styles.error}>{program.error.message}</p>;
  }
  if (!program.data) {
    return (
      <div className={styles.page}>
        <PageHeader title="Программа" back={acting ? undefined : { to: '/profile', label: 'Профиль' }} />
        <EmptyState
          title="Программы пока нет"
          text={
            acting
              ? 'Клиент ещё не проходил анкету.'
              : 'Ответьте на 4 коротких вопроса — подберём упражнения, подходы и дни недели.'
          }
          action={acting ? undefined : { to: '/program/new', label: 'Подобрать программу' }}
        />
      </div>
    );
  }
  return (
    <ProgramView program={program.data} created={created} consultDoctor={consultDoctor} acting={Boolean(acting)} />
  );
}

function ProgramView({
  program,
  created,
  consultDoctor,
  acting,
}: {
  program: Program;
  created: boolean;
  consultDoctor: boolean;
  acting: boolean;
}) {
  const top = headline(program);
  const rest = restDaysText(program.days);
  const week = programWeek(program.startedAt);

  return (
    <div className={styles.page}>
      <PageHeader
        eyebrow={created ? 'Готово' : undefined}
        title={acting ? 'Программа клиента' : 'Ваша программа'}
        subtitle={programSummary(program)}
        back={created || acting ? undefined : { to: '/profile', label: 'Профиль' }}
      />

      <PageColumns
        aside={
          <section className={styles.section} aria-labelledby="why">
            <SectionLabel id="why">Почему так</SectionLabel>
            <ListRowGroup>
              {programReasons(program).map((r) => (
                <div key={r.title} className={styles.reason}>
                  <span className={styles.reasonTitle}>{r.title}</span>
                  <span className={styles.muted}>{r.text}</span>
                  <span className={styles.source}>{r.source}</span>
                </div>
              ))}
            </ListRowGroup>
          </section>
        }
      >
        {consultDoctor && (
          <div className={styles.warning} role="status">
            <strong>Посоветуйтесь с врачом перед началом</strong>
            <span>Начните с разрешения врача и с лёгких весов.</span>
          </div>
        )}

        <Card variant="inverse" size="section" className={styles.hero}>
          <div className={styles.heroHead}>
            <span className={styles.heroTitle}>{program.title}</span>
            <span className={styles.heroMeta}>
              {program.reviewDue
                ? 'пора пересмотреть'
                : `неделя ${week} из 6 · до ${formatShortDate(`${program.reviewAt}T12:00:00`)}`}
            </span>
          </div>
          {top && (
            <dl className={styles.stats}>
              <div>
                <dt>упражнений за тренировку</dt>
                <dd>{top.exercises}</dd>
              </div>
              <div>
                <dt>повторов, 1–3 в запасе</dt>
                <dd>{top.reps}</dd>
              </div>
              <div>
                <dt>отдых в базовых</dt>
                <dd>{top.rest}</dd>
              </div>
            </dl>
          )}
        </Card>

        {program.notes.length > 0 && (
          <Card as="section" className={styles.notes} aria-label="Что учли при подборе">
            {program.notes.map((note) => (
              <p key={note} className={styles.muted}>
                {note}
              </p>
            ))}
          </Card>
        )}

        <section className={styles.section} aria-labelledby="week">
          <SectionLabel id="week" action={<Link to="/plan">План недели →</Link>}>
            Неделя
          </SectionLabel>
          <div className={styles.days}>
            {program.days.map((day, i) => (
              <DayCard key={day.templateId} day={day} open={i === 0} />
            ))}
          </div>
          {rest && <p className={styles.muted}>{rest} — отдых. Дни можно переставить в плане недели.</p>}
        </section>
      </PageColumns>

      {/* кнопки — в самом конце: на телефоне колонка «Почему так» встаёт выше них */}
      {created ? (
        <div className={styles.actions}>
          <ButtonLink to="/" fullWidth>
            Начать программу
          </ButtonLink>
          <ButtonLink to="/program/new" variant="secondary" fullWidth>
            Изменить ответы
          </ButtonLink>
          <p className={styles.footnote}>План недели — уже из дней программы. Ваши прежние комплексы остались.</p>
        </div>
      ) : (
        !acting && (
          <div className={styles.actions}>
            <ButtonLink to="/program/new" variant="secondary" fullWidth>
              Пройти анкету заново
            </ButtonLink>
          </div>
        )
      )}
    </div>
  );
}

/** День программы: свёрнут — дни недели, название и первые упражнения; развёрнут — все места со схемой */
function DayCard({ day, open }: { day: ProgramDay; open: boolean }) {
  const when = weekDaysText(day.weekDays);
  return (
    <details className={styles.day} open={open}>
      <summary className={styles.daySummary}>
        <span className={styles.dayMark} aria-hidden="true">
          {when ? when.split(' · ')[0] : '—'}
        </span>
        <span className={styles.dayBody}>
          <span className={styles.dayName}>{day.name}</span>
          <span className={styles.muted}>
            {/* один день недели уже в значке слева; несколько (Пн · Пт) — пишем все */}
            {!when ? 'нет в плане · ' : day.weekDays.length > 1 ? `${when} · ` : ''}
            {dayPreview(day)}
          </span>
        </span>
        <span className={styles.chevron} aria-hidden="true">
          ›
        </span>
      </summary>
      <ListRowGroup label={day.name}>
        {day.slots.map((slot) => (
          <ListRow
            key={slot.id}
            mark={String(slot.position + 1)}
            title={slot.exerciseName}
            text={
              slot.swapped && slot.originalExerciseName
                ? `${slotScheme(slot)} · вместо: ${slot.originalExerciseName.toLowerCase()}`
                : slotScheme(slot)
            }
            value={slot.swapped ? <Badge>заменено</Badge> : undefined}
          />
        ))}
      </ListRowGroup>
    </details>
  );
}
