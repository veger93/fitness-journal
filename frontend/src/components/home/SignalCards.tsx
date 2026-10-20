import { useState } from 'react';
import type { ExerciseStatus, ExerciseSummary } from '../../api/analytics';
import { formatSet } from '../../lib/format';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { DeloadDialog } from './DeloadDialog';
import { PlateauDialog } from './PlateauDialog';
import styles from './Home.module.css';

const MAX_SIGNALS = 3;

interface Signal {
  variant: 'accent' | 'default' | 'inverse';
  title: string;
  text: string;
  action: { label: string; dialog: 'deload' | 'plateau' } | null;
}

/** Текст карточки по статусу. Цифры — из сервера (note), формулировка — здесь. */
function toSignal(exercise: ExerciseSummary): Signal | null {
  const statuses: Record<Exclude<ExerciseStatus, 'NORMAL'>, () => Signal> = {
    OVERLOAD: () => ({
      variant: 'accent',
      title: 'Пора неделю полегче',
      text: exercise.note ?? 'Похоже, накопилась усталость.',
      action: { label: 'План разгрузки', dialog: 'deload' },
    }),
    PLATEAU: () => ({
      variant: 'default',
      title: exercise.note ? `Плато: ${exercise.note.replace(' без нового максимума', '')}` : 'Плато',
      text: 'Вес не растёт, хотя тренировки идут. Это нормально — помогут небольшие перемены.',
      action: { label: 'Что попробовать', dialog: 'plateau' },
    }),
    RECORD: () => ({
      variant: 'inverse',
      title: `Рекорд: ${formatSet(exercise.lastWeightKg, exercise.lastReps) ?? ''}`,
      text: `${exercise.note ?? 'Новый личный рекорд'}. Так держать!`,
      action: null,
    }),
  };
  return exercise.status === 'NORMAL' ? null : statuses[exercise.status]();
}

/** "Что сделать сейчас": только упражнения, требующие внимания (сервер уже отсортировал по важности). */
export function SignalCards({ exercises }: { exercises: ExerciseSummary[] }) {
  const [open, setOpen] = useState<{ dialog: 'deload' | 'plateau'; exercise: ExerciseSummary } | null>(null);
  const signals = exercises
    .map((exercise) => ({ exercise, signal: toSignal(exercise) }))
    .filter((s): s is { exercise: ExerciseSummary; signal: Signal } => s.signal !== null)
    .slice(0, MAX_SIGNALS);

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionTitle}>Что сделать сейчас</h2>
      {signals.length === 0 ? (
        <Card>
          <p className={styles.signalText}>Всё идёт по плану: ни перегрузки, ни плато. Продолжайте в том же духе.</p>
        </Card>
      ) : (
        <div className={styles.signals}>
          {signals.map(({ exercise, signal }) => (
            <Card key={exercise.exerciseId} as="article" variant={signal.variant} className={styles.signal}>
              <span className={styles.signalLabel}>{exercise.exerciseName}</span>
              <h3 className={styles.signalTitle}>{signal.title}</h3>
              <p className={styles.signalText}>{signal.text}</p>
              {signal.action && (
                <Button
                  variant={signal.variant === 'accent' ? 'surface' : 'secondary'}
                  className={styles.signalAction}
                  onClick={() => setOpen({ dialog: signal.action!.dialog, exercise })}
                >
                  {signal.action.label}
                </Button>
              )}
            </Card>
          ))}
        </div>
      )}

      {open?.dialog === 'deload' && <DeloadDialog exercise={open.exercise} onClose={() => setOpen(null)} />}
      {open?.dialog === 'plateau' && <PlateauDialog exercise={open.exercise} onClose={() => setOpen(null)} />}
    </section>
  );
}
