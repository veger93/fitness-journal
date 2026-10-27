import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationsApi, type NotificationSettings, type ReminderMode } from '../../api/notifications';
import { workoutsApi, type DayOfWeek, type WeeklyPlan } from '../../api/workouts';
import { DAYS } from '../../lib/plan';
import { deviceTimeZone, restDaysAmong, toTimeInput, toggleDay } from '../../lib/reminders';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Chip } from '../ui/Chip';
import { SegmentedControl } from '../ui/SegmentedControl';
import { Stepper } from '../ui/Stepper';
import { Switch } from '../ui/Switch';
import styles from './Profile.module.css';

const MODES: { value: ReminderMode; label: string }[] = [
  { value: 'PLAN', label: 'Как в плане' },
  { value: 'CUSTOM', label: 'Свои дни' },
];

/** Напоминания: в какие дни и во сколько, и письмо "давно не было тренировок". Аналитические уведомления включены всегда. */
export function RemindersCard() {
  const settings = useQuery({ queryKey: ['notifications', 'settings'], queryFn: notificationsApi.settings });
  const plan = useQuery({ queryKey: ['plan'], queryFn: workoutsApi.plan });
  return (
    <Card>
      <h2 className={styles.sectionTitle}>Напоминания</h2>
      {settings.data ? (
        <RemindersForm initial={settings.data} plan={plan.data} />
      ) : (
        <p className={styles.muted}>{settings.isError ? settings.error.message : 'Загружаем…'}</p>
      )}
    </Card>
  );
}

function RemindersForm({ initial, plan }: { initial: NotificationSettings; plan: WeeklyPlan | undefined }) {
  const queryClient = useQueryClient();
  const [enabled, setEnabled] = useState(initial.remindersEnabled);
  const [mode, setMode] = useState<ReminderMode>(initial.reminderMode);
  const [days, setDays] = useState(initial.reminderDays);
  const [time, setTime] = useState(toTimeInput(initial.reminderTime));
  const [inactivity, setInactivity] = useState(initial.inactivityEnabled);
  const [inactivityDays, setInactivityDays] = useState(initial.inactivityDays);
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: notificationsApi.updateSettings,
    onSuccess: (saved) => queryClient.setQueryData(['notifications', 'settings'], saved),
    onError: (e) => setError(e.message),
  });

  const onSave = () => {
    setError(null);
    if (enabled && mode === 'CUSTOM' && days.length === 0) {
      setError('Выберите хотя бы один день.');
      return;
    }
    save.mutate({
      remindersEnabled: enabled,
      reminderMode: mode,
      reminderDays: days,
      reminderTime: time,
      inactivityEnabled: inactivity,
      inactivityDays,
      // берём с устройства при каждом сохранении: переехал в другой пояс — напоминания подстроятся
      timeZone: deviceTimeZone(),
    });
  };

  // любое изменение снимает "Сохранено ✓"
  const touched =
    <T,>(set: (value: T) => void) =>
    (value: T) => {
      set(value);
      save.reset();
    };

  return (
    <div className={styles.form}>
      <Switch
        label="Напоминать о тренировках"
        hint="Письмо в день тренировки"
        checked={enabled}
        onChange={touched(setEnabled)}
      />
      {enabled && (
        <>
          <SegmentedControl legend="Дни напоминаний" options={MODES} value={mode} onChange={touched(setMode)} />
          {mode === 'PLAN' ? (
            <PlanDays plan={plan} />
          ) : (
            <CustomDays days={days} plan={plan} onChange={touched(setDays)} />
          )}
          <label className={styles.timeField}>
            <span className={styles.legend}>Время</span>
            <input
              type="time"
              className={styles.timeInput}
              value={time}
              onChange={(e) => touched(setTime)(e.target.value)}
            />
          </label>
        </>
      )}
      <Switch
        label="Если давно не было тренировок"
        hint="Одно письмо, если перерыв затянулся"
        checked={inactivity}
        onChange={touched(setInactivity)}
      />
      {inactivity && (
        <Stepper
          label="Перерыв, дней"
          value={inactivityDays}
          onChange={touched(setInactivityDays)}
          step={1}
          min={3}
          max={30}
        />
      )}
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <Button variant="secondary" disabled={save.isPending} onClick={onSave}>
        {save.isSuccess ? 'Сохранено ✓' : 'Сохранить'}
      </Button>
      {/* проверка — только сразу после сохранения включённых напоминаний: в обычном профиле лишняя кнопка не нужна */}
      {save.isSuccess && save.data.remindersEnabled && <TestReminder />}
      <p className="footnote">
        Если в этот день тренировка уже записана, напоминание не придёт. Рекорды, плато, перегрузки и итоги месяца
        приходят всегда. Часовой пояс: {deviceTimeZone()}.
      </p>
    </div>
  );
}

/** Режим "как в плане": дни только для чтения — меняются на странице плана, и напоминания следуют за ним сами. */
function PlanDays({ plan }: { plan: WeeklyPlan | undefined }) {
  const planned = (plan?.days ?? []).filter((d) => d.template !== null);
  if (planned.length === 0) {
    return (
      <p className={styles.warning}>
        План на неделю пуст — напоминать не о чем. <Link to="/plan">Составить план</Link>
      </p>
    );
  }
  return (
    <div className={styles.days}>
      <ul className={styles.planList}>
        {planned.map((d) => (
          <li key={d.dayOfWeek}>
            <span className={styles.muted}>{DAYS.find((day) => day.day === d.dayOfWeek)?.name}</span>
            <span>{d.template?.name}</span>
          </li>
        ))}
      </ul>
      <p className={styles.muted}>
        В дни отдыха напоминаний нет. Поменяете <Link to="/plan">план</Link> — напоминания подстроятся сами.
      </p>
    </div>
  );
}

function CustomDays({
  days,
  plan,
  onChange,
}: {
  days: DayOfWeek[];
  plan: WeeklyPlan | undefined;
  onChange: (days: DayOfWeek[]) => void;
}) {
  const rest = restDaysAmong(days, plan);
  return (
    <fieldset className={styles.days}>
      <legend className={styles.legend}>Дни</legend>
      <div className={styles.chips}>
        {DAYS.map((d) => (
          <Chip
            key={d.day}
            selected={days.includes(d.day)}
            aria-label={d.name}
            onClick={() => onChange(toggleDay(days, d.day))}
          >
            {d.short}
          </Chip>
        ))}
      </div>
      {rest.length > 0 && (
        <p className={styles.muted}>
          {rest.map((d) => DAYS.find((day) => day.day === d)?.short).join(', ')} — по плану отдых. Напоминание придёт и
          в эти дни, но без «по плану».
        </p>
      )}
    </fieldset>
  );
}

/**
 * Пробное напоминание: проверка, что вся цепочка работает (адрес почты, почтовый сервер).
 * Показывается один раз — после сохранения настроек, когда человек как раз хочет убедиться, что всё настроено.
 */
function TestReminder() {
  const queryClient = useQueryClient();
  const test = useMutation({
    mutationFn: notificationsApi.testReminder,
    // письмо попало и в ленту — колокольчик должен это показать
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });
  return (
    <div className={styles.days}>
      <button type="button" className={styles.linkButton} disabled={test.isPending} onClick={() => test.mutate()}>
        {test.isPending ? 'Отправляем…' : 'Прислать пробное напоминание сейчас'}
      </button>
      {test.isSuccess && test.data.status === 'SENT' && (
        <p className={styles.muted}>Письмо отправлено — оно же появилось в уведомлениях.</p>
      )}
      {test.isSuccess && test.data.status === 'SKIPPED' && (
        <p className={styles.error}>Письмо не отправлено: {test.data.reason}</p>
      )}
      {test.isError && <p className={styles.error}>{test.error.message}</p>}
    </div>
  );
}
