import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationsApi, type NotificationSettings } from '../../api/notifications';
import { workoutsApi } from '../../api/workouts';
import { DAYS } from '../../lib/plan';
import { deviceTimeZone, planDays, sameDays, toTimeInput, toggleDay } from '../../lib/reminders';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Chip } from '../ui/Chip';
import { Stepper } from '../ui/Stepper';
import { Switch } from '../ui/Switch';
import styles from './Profile.module.css';

/** Напоминания: в какие дни и во сколько, и письмо "давно не было тренировок". Аналитические уведомления включены всегда. */
export function RemindersCard() {
  const settings = useQuery({ queryKey: ['notifications', 'settings'], queryFn: notificationsApi.settings });
  return (
    <Card>
      <h2 className={styles.sectionTitle}>Напоминания</h2>
      {settings.data ? (
        <RemindersForm initial={settings.data} />
      ) : (
        <p className={styles.muted}>{settings.isError ? settings.error.message : 'Загружаем…'}</p>
      )}
    </Card>
  );
}

function RemindersForm({ initial }: { initial: NotificationSettings }) {
  const queryClient = useQueryClient();
  const plan = useQuery({ queryKey: ['plan'], queryFn: workoutsApi.plan });
  const fromPlan = planDays(plan.data);

  const [enabled, setEnabled] = useState(initial.remindersEnabled);
  // впервые включает напоминания — предлагаем дни из плана на неделю
  const [days, setDays] = useState(initial.reminderDays.length > 0 ? initial.reminderDays : fromPlan);
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
    if (enabled && days.length === 0) {
      setError('Выберите хотя бы один день.');
      return;
    }
    save.mutate({
      remindersEnabled: enabled,
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
        hint="Письмо в выбранные дни"
        checked={enabled}
        onChange={(checked) => {
          touched(setEnabled)(checked);
          // включил впервые — сразу подставляем дни из плана на неделю (план мог загрузиться позже формы)
          if (checked && days.length === 0) {
            setDays(fromPlan);
          }
        }}
      />
      {enabled && (
        <>
          <fieldset className={styles.days}>
            <legend className={styles.legend}>Дни</legend>
            <div className={styles.chips}>
              {DAYS.map((d) => (
                <Chip
                  key={d.day}
                  selected={days.includes(d.day)}
                  aria-label={d.name}
                  onClick={() => touched(setDays)(toggleDay(days, d.day))}
                >
                  {d.short}
                </Chip>
              ))}
            </div>
            {fromPlan.length > 0 && !sameDays(days, fromPlan) && (
              <button type="button" className={styles.linkButton} onClick={() => touched(setDays)(fromPlan)}>
                Как в плане на неделю
              </button>
            )}
            {fromPlan.length === 0 && (
              <p className={styles.muted}>
                Совет: составьте <Link to="/plan">план на неделю</Link> — дни напоминаний можно взять из него.
              </p>
            )}
          </fieldset>
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
      <p className="footnote">
        Рекорды, плато, перегрузки и итоги месяца приходят всегда — это главные подсказки дневника. Часовой пояс:{' '}
        {deviceTimeZone()}.
      </p>
    </div>
  );
}
