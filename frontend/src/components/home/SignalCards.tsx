import { useState } from 'react';
import type { ExerciseSummary } from '../../api/analytics';
import { formatExerciseResult } from '../../lib/format';
import { ListRow, ListRowGroup } from '../ui/ListRow';
import { SectionLabel } from '../ui/SectionLabel';
import { DeloadDialog } from './DeloadDialog';
import { PlateauDialog } from './PlateauDialog';
import styles from './Home.module.css';

/**
 * "Что сделать сейчас" — строки в одной карточке (дизайн-система: ListRow), на телефоне и на ПК одинаково:
 *   перегрузка (оранжевый значок) -> план разгрузки; плато -> что попробовать; рекорды -> все рекорды.
 * Блок есть только когда есть перегрузка или плато: рекорды без них — точка на вкладке "Рекорды" (useNewRecord).
 * Спокойные темы — одной строкой "✓ Перегрузки нет".
 */
export function SignalCards({ exercises }: { exercises: ExerciseSummary[] }) {
  const [open, setOpen] = useState<{ dialog: 'deload' | 'plateau'; exercise: ExerciseSummary } | null>(null);
  const overloads = exercises.filter((e) => e.status === 'OVERLOAD');
  const plateaus = exercises.filter((e) => e.status === 'PLATEAU');
  const records = exercises.filter((e) => e.status === 'RECORD');

  if (overloads.length === 0 && plateaus.length === 0) {
    return null;
  }
  const calm = [overloads.length === 0 && 'перегрузки нет', plateaus.length === 0 && 'плато нет'].filter(Boolean);
  const running = overloads[0]?.trackingType === 'DISTANCE';

  return (
    <section className={styles.section} aria-labelledby="signals-title">
      <SectionLabel id="signals-title">Что сделать сейчас</SectionLabel>
      <ListRowGroup>
        {overloads.length > 0 && (
          <ListRow
            mark="!"
            tone="accent"
            title={running ? 'Притормозите с километражем' : 'Пора неделю полегче'}
            text={`Перегрузка · ${withMore(overloads)}`}
            {...(running
              ? { to: '/running' }
              : { onClick: () => setOpen({ dialog: 'deload', exercise: overloads[0] }) })}
          />
        )}
        {plateaus.length > 0 && (
          <ListRow
            mark="≈"
            title={plateauTitle(plateaus[0])}
            text={withMore(plateaus)}
            onClick={() => setOpen({ dialog: 'plateau', exercise: plateaus[0] })}
          />
        )}
        {records.length > 0 && (
          <ListRow
            mark="★"
            tone="inverse"
            title={
              records.length === 1 ? `Рекорд: ${formatExerciseResult(records[0]) ?? ''}` : `Рекордов: ${records.length}`
            }
            text={records.length === 1 ? `${records[0].exerciseName} · все рекорды` : 'за две недели · все рекорды'}
            to="/records"
          />
        )}
        {calm.length > 0 && (
          <p className={styles.calm}>
            <span aria-hidden="true">✓</span> {capitalize(calm.join(' · '))}
          </p>
        )}
      </ListRowGroup>
      {open?.dialog === 'deload' && <DeloadDialog exercise={open.exercise} onClose={() => setOpen(null)} />}
      {open?.dialog === 'plateau' && <PlateauDialog exercise={open.exercise} onClose={() => setOpen(null)} />}
    </section>
  );
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
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
