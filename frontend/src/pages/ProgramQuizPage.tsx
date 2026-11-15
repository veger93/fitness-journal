import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import {
  programsApi,
  type BodyLimit,
  type PriorityGroup,
  type Program,
  type ProgramGoal,
  type ProgramLocation,
} from '../api/programs';
import { usersApi } from '../api/users';
import { Button } from '../components/ui/Button';
import { ChoiceCards } from '../components/ui/ChoiceCards';
import { Chip } from '../components/ui/Chip';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import {
  DAY_OPTIONS,
  GOALS,
  LIMITS,
  LOCATIONS,
  MINUTE_OPTIONS,
  PRIORITIES,
  SAFETY_QUESTIONS,
  minutesHint,
  splitHint,
} from '../lib/program';
import { useActingClient } from '../trainer/ActingClientProvider';
import styles from './ProgramQuizPage.module.css';

const STEPS = 4;
type Answer = 'yes' | 'no' | '';

/** Что передаём экрану программы после создания (через state навигации, не в адресе) */
export interface ProgramCreatedState {
  created: true;
  /** хоть одно «да» в вопросах безопасности. Сами ответы никуда не отправляем */
  consultDoctor: boolean;
}

/**
 * Анкета программы, вариант A: по одному вопросу на экран (docs/program.md, макеты A1–A4).
 * Открывается сразу после онбординга (?onboarding=1) и из профиля. «Я сам» — пропустить без программы.
 * Есть программа — подставляем её ответы: пересмотр = поправить пару пунктов, а не заполнять всё заново.
 */
export function ProgramQuizPage() {
  const [params] = useSearchParams();
  const onboarding = params.has('onboarding');
  const { client: acting } = useActingClient();
  const profile = useQuery({ queryKey: ['profile'], queryFn: usersApi.profile });
  const current = useQuery({ queryKey: ['program'], queryFn: programsApi.current, enabled: !acting });

  // опыт берём из профиля клиента, а у тренера в режиме клиента под рукой только свой — анкету клиент проходит сам
  if (acting) {
    return <Navigate to="/program" replace />;
  }
  const leaveTo = onboarding ? '/' : '/profile';
  if (profile.isPending || current.isPending) {
    return <div className={styles.page} aria-busy="true" />;
  }
  return (
    <Quiz program={current.data ?? null} experience={profile.data?.experienceLevel ?? 'BEGINNER'} leaveTo={leaveTo} />
  );
}

function Quiz({
  program,
  experience,
  leaveTo,
}: {
  program: Program | null;
  experience: NonNullable<Program['experienceLevel']>;
  leaveTo: string;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [step, setStep] = useState(1);
  const [goal, setGoal] = useState<ProgramGoal | null>(program?.goal ?? null);
  const [days, setDays] = useState(String(program?.daysPerWeek ?? 3));
  const [minutes, setMinutes] = useState(String(program?.sessionMinutes ?? 60));
  const [location, setLocation] = useState<ProgramLocation>(program?.location ?? 'GYM');
  const [limits, setLimits] = useState<BodyLimit[]>(program?.limits ?? []);
  const [priority, setPriority] = useState<PriorityGroup | null>(program?.priority ?? null);
  // вопросы безопасности — каждый раз заново и только в памяти страницы
  const [safety, setSafety] = useState<Answer[]>(SAFETY_QUESTIONS.map(() => ''));

  const consultDoctor = safety.includes('yes');
  const safetyDone = !safety.includes('');

  const create = useMutation({
    mutationFn: () =>
      programsApi.create({
        goal: goal!,
        daysPerWeek: Number(days),
        sessionMinutes: Number(minutes),
        location,
        experienceLevel: experience,
        limits,
        priority,
      }),
    onSuccess: (created) => {
      queryClient.setQueryData(['program'], created);
      // программа поменяла план недели и добавила комплексы — главная и «План» должны это увидеть
      void queryClient.invalidateQueries({ queryKey: ['plan'] });
      void queryClient.invalidateQueries({ queryKey: ['templates'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      const state: ProgramCreatedState = { created: true, consultDoctor };
      navigate('/program', { replace: true, state });
    },
  });

  const toggleLimit = (limit: BodyLimit) =>
    setLimits((all) => (all.includes(limit) ? all.filter((l) => l !== limit) : [...all, limit]));

  const canGoNext = step === 1 ? goal !== null : true;
  const hint = splitHint(Number(days));

  return (
    <div className={styles.page}>
      <div className={styles.box}>
        <div className={styles.top}>
          <div className={styles.topRow}>
            {step > 1 ? (
              <button type="button" className={styles.textButton} onClick={() => setStep(step - 1)}>
                ← Назад
              </button>
            ) : (
              <span />
            )}
            <span className={styles.stepLabel}>
              Шаг {step} из {STEPS}
            </span>
            <Link to={leaveTo} className={styles.textButton}>
              {program ? 'Отмена' : 'Я сам — пропустить'}
            </Link>
          </div>
          <div
            className={styles.progress}
            role="progressbar"
            aria-valuemin={1}
            aria-valuemax={STEPS}
            aria-valuenow={step}
            aria-label="Шаги анкеты"
          >
            {Array.from({ length: STEPS }, (_, i) => (
              <span key={i} className={i < step ? styles.done : undefined} />
            ))}
          </div>
        </div>

        {step === 1 && (
          <section className={styles.step}>
            <header className={styles.head}>
              <h1 className={styles.title}>Чего хотите добиться?</h1>
              <p className={styles.muted}>Подберём повторы и акценты. Цель можно поменять при пересмотре программы.</p>
            </header>
            <ChoiceCards legend="Цель" hideLegend options={GOALS} value={goal} onChange={setGoal} />
          </section>
        )}

        {step === 2 && (
          <section className={styles.step}>
            <header className={styles.head}>
              <h1 className={styles.title}>Сколько готовы тренироваться?</h1>
              <p className={styles.muted}>Честно, с запасом: лучше 3 дня каждую неделю, чем 5, но через раз.</p>
            </header>
            <div className={styles.group}>
              <h2 className={styles.label}>Дней в неделю</h2>
              <SegmentedControl
                legend="Дней в неделю"
                options={DAY_OPTIONS.map((d) => ({ value: String(d), label: String(d) }))}
                value={days}
                onChange={setDays}
              />
              <div className={styles.hint}>
                <span className={styles.hintTitle}>{hint.title}</span>
                <span className={styles.muted}>{hint.text}</span>
              </div>
            </div>
            <div className={styles.group}>
              <h2 className={styles.label}>Время на тренировку</h2>
              <SegmentedControl
                legend="Время на тренировку"
                options={[...MINUTE_OPTIONS]}
                value={minutes}
                onChange={setMinutes}
              />
              <p className={styles.muted}>{minutesHint(Number(minutes))}</p>
            </div>
          </section>
        )}

        {step === 3 && (
          <section className={styles.step}>
            <header className={styles.head}>
              <h1 className={styles.title}>Где тренируетесь?</h1>
            </header>
            <ChoiceCards
              legend="Где тренируетесь"
              hideLegend
              options={LOCATIONS}
              value={location}
              onChange={setLocation}
            />
            <div className={styles.group}>
              <h2 className={styles.label}>Что-то беспокоит? Необязательно</h2>
              <div className={styles.chips}>
                {LIMITS.map((l) => (
                  <Chip key={l.value} selected={limits.includes(l.value)} onClick={() => toggleLimit(l.value)}>
                    {l.label}
                  </Chip>
                ))}
              </div>
              {limits.length > 0 && (
                <p className={styles.muted}>
                  Уберём упражнения:{' '}
                  {LIMITS.filter((l) => limits.includes(l.value))
                    .map((l) => l.excludes)
                    .join('; ')}
                  . Это не медицинский совет.
                </p>
              )}
            </div>
            <div className={styles.group}>
              <h2 className={styles.label}>Что подтянуть в первую очередь? Необязательно</h2>
              <div className={styles.chips}>
                {PRIORITIES.map((p) => (
                  <Chip key={p.label} selected={priority === p.value} onClick={() => setPriority(p.value)}>
                    {p.label}
                  </Chip>
                ))}
              </div>
            </div>
          </section>
        )}

        {step === 4 && (
          <section className={styles.step}>
            <header className={styles.head}>
              <h1 className={styles.title}>Пара вопросов о здоровье</h1>
              <p className={styles.muted}>Ответы никуда не отправляем и не сохраняем.</p>
            </header>
            <div className={styles.questions}>
              {SAFETY_QUESTIONS.map((question, i) => (
                <div key={question} className={styles.question}>
                  <span id={`safety-${i}`}>{question}</span>
                  <div className={styles.yesNo}>
                    <SegmentedControl<Answer>
                      legend={question}
                      options={[
                        { value: 'yes', label: 'Да' },
                        { value: 'no', label: 'Нет' },
                      ]}
                      value={safety[i]}
                      onChange={(value) => setSafety((all) => all.map((a, j) => (j === i ? value : a)))}
                    />
                  </div>
                </div>
              ))}
            </div>
            {consultDoctor && (
              <div className={styles.warning} role="status">
                <strong>Посоветуйтесь с врачом перед началом</strong>
                <span>Программу составим, но начните с разрешения врача и с лёгких весов.</span>
              </div>
            )}
          </section>
        )}

        <div className={styles.bottom}>
          {create.isError && <p className={styles.error}>{create.error.message}</p>}
          {step < STEPS ? (
            <Button fullWidth disabled={!canGoNext} onClick={() => setStep(step + 1)}>
              Далее
            </Button>
          ) : (
            <Button fullWidth disabled={!safetyDone || create.isPending} onClick={() => create.mutate()}>
              {create.isPending ? 'Составляем…' : 'Составить программу'}
            </Button>
          )}
          <p className={styles.footnote}>
            {step === 1 && '4 коротких шага, около минуты'}
            {step === 2 && '* ACSM, 2009: новичкам 2–3 тренировки в неделю.'}
            {step === 4 &&
              (program
                ? 'Текущая программа уйдёт в архив, план недели заменим днями новой. Ваши комплексы останутся.'
                : 'План недели заменим днями программы. Ваши комплексы останутся.')}
          </p>
        </div>
      </div>
    </div>
  );
}
