import { useState } from 'react';
import type { ExerciseSummary } from '../../api/analytics';
import { formatExerciseResult } from '../../lib/format';
import { Button, ButtonLink } from '../ui/Button';
import { Card } from '../ui/Card';
import { DeloadDialog } from './DeloadDialog';
import { PlateauDialog } from './PlateauDialog';
import styles from './Home.module.css';

/**
 * "Что сделать сейчас" — три постоянных места, у каждого своя роль:
 *   1. перегрузка (оранжевая) -> план разгрузки;
 *   2. плато (обычная карточка) -> что попробовать;
 *   3. рекорды (акцентная тёмная/светлая) -> все рекорды.
 * Если по своей теме сказать нечего, карточка остаётся на месте, но спокойного цвета и с хорошей новостью:
 * оранжевый — только когда действительно пора обратить внимание.
 */
export function SignalCards({ exercises }: { exercises: ExerciseSummary[] }) {
  const [open, setOpen] = useState<{ dialog: 'deload' | 'plateau'; exercise: ExerciseSummary } | null>(null);
  const overloads = exercises.filter((e) => e.status === 'OVERLOAD');
  const plateaus = exercises.filter((e) => e.status === 'PLATEAU');
  const records = exercises.filter((e) => e.status === 'RECORD');

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionTitle}>Что сделать сейчас</h2>
      <div className={styles.signals}>
        {overloads.length > 0 ? (
          <Card as="article" variant="accent" className={styles.signal}>
            <span className={styles.signalLabel}>{withMore(overloads)}</span>
            <h3 className={styles.signalTitle}>Пора неделю полегче</h3>
            <p className={styles.signalText}>{overloads[0].note ?? 'Похоже, накопилась усталость.'}</p>
            <Button
              variant="surface"
              className={styles.signalAction}
              onClick={() => setOpen({ dialog: 'deload', exercise: overloads[0] })}
            >
              План разгрузки
            </Button>
          </Card>
        ) : (
          <Card as="article" className={styles.signal}>
            <span className={styles.signalLabel}>Восстановление</span>
            <h3 className={styles.signalTitle}>Перегрузки нет</h3>
            <p className={styles.signalText}>Нагрузка растёт в нормальном темпе — отдых не нужен.</p>
          </Card>
        )}

        {plateaus.length > 0 ? (
          <Card as="article" className={styles.signal}>
            <span className={styles.signalLabel}>{withMore(plateaus)}</span>
            <h3 className={styles.signalTitle}>{plateauTitle(plateaus[0])}</h3>
            <p className={styles.signalText}>Вес не растёт, хотя тренировки идут. Помогут небольшие перемены.</p>
            <Button
              variant="secondary"
              className={styles.signalAction}
              onClick={() => setOpen({ dialog: 'plateau', exercise: plateaus[0] })}
            >
              Что попробовать
            </Button>
          </Card>
        ) : (
          <Card as="article" className={styles.signal}>
            <span className={styles.signalLabel}>Прогресс</span>
            <h3 className={styles.signalTitle}>Плато нет</h3>
            <p className={styles.signalText}>Во всех упражнениях вес растёт или пока рано делать выводы.</p>
          </Card>
        )}

        <Card as="article" variant="inverse" className={styles.signal}>
          {records.length > 0 ? (
            <>
              <span className={styles.signalLabel}>{withMore(records)}</span>
              <h3 className={styles.signalTitle}>
                {records.length === 1
                  ? `Рекорд: ${formatExerciseResult(records[0]) ?? ''}`
                  : `Рекордов: ${records.length}`}
              </h3>
              <p className={styles.signalText}>
                {records.length === 1
                  ? `${records[0].note ?? 'Новый рекорд'}. Так держать!`
                  : 'За последние две недели. Так держать!'}
              </p>
            </>
          ) : (
            <>
              <span className={styles.signalLabel}>Рекорды</span>
              <h3 className={styles.signalTitle}>Новых рекордов нет</h3>
              <p className={styles.signalText}>Здесь появятся рекорды за последние две недели.</p>
            </>
          )}
          <ButtonLink to="/records" variant="secondary" className={styles.signalAction}>
            Все рекорды
          </ButtonLink>
        </Card>
      </div>

      {open?.dialog === 'deload' && <DeloadDialog exercise={open.exercise} onClose={() => setOpen(null)} />}
      {open?.dialog === 'plateau' && <PlateauDialog exercise={open.exercise} onClose={() => setOpen(null)} />}
    </section>
  );
}

/** "Приседания со штангой" или "Приседания со штангой и ещё 2" */
function withMore(list: ExerciseSummary[]): string {
  return list.length > 1 ? `${list[0].exerciseName} и ещё ${list.length - 1}` : list[0].exerciseName;
}

/** note с сервера: "24 дн. без нового максимума" -> "Плато 24 дн." */
function plateauTitle(exercise: ExerciseSummary): string {
  const days = exercise.note?.match(/^(\d+)/)?.[1];
  return days ? `Плато ${days} дн.` : 'Плато';
}
