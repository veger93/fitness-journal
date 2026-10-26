import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { localDate, usersApi } from '../../api/users';
import { formatChange, formatKg, formatShortDate } from '../../lib/format';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Sparkline } from '../ui/Sparkline';
import { Stepper } from '../ui/Stepper';
import styles from './Profile.module.css';

/** Сколько последних записей показать списком (на графике — больше) */
const LIST_SIZE = 5;
const CHART_SIZE = 20;

/** Вес тела: записать сегодняшний и посмотреть, как он менялся. */
export function BodyWeightCard({ currentWeightKg }: { currentWeightKg: number | null }) {
  const queryClient = useQueryClient();
  const history = useQuery({ queryKey: ['weight'], queryFn: usersApi.weightHistory });
  const [weight, setWeight] = useState(currentWeightKg ?? 75);

  const record = useMutation({
    mutationFn: () => usersApi.recordWeight(localDate(), weight),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['weight'] });
      // в профиле "текущий вес" = последняя запись
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });

  const entries = history.data ?? []; // сервер отдаёт новые сверху
  // для графика нужен порядок "старые слева": разворачиваем КОПИЮ — данные из кэша не трогаем
  const trend = entries
    .slice(0, CHART_SIZE)
    .map((e) => e.weightKg)
    .reverse();
  const change = entries.length > 1 ? ((entries[0].weightKg - entries[1].weightKg) / entries[1].weightKg) * 100 : null;

  return (
    <Card>
      <h2 className={styles.sectionTitle}>Вес тела</h2>
      <div className={styles.weightRow}>
        <Stepper
          label="Сегодня, кг"
          value={weight}
          onChange={(value) => {
            setWeight(value);
            record.reset(); // новое значение ещё не записано — убираем "Записано ✓"
          }}
          step={0.5}
          min={20.5}
          max={399.5}
          inputMode="decimal"
        />
        <Button variant="secondary" disabled={record.isPending} onClick={() => record.mutate()}>
          {record.isSuccess ? 'Записано ✓' : 'Записать'}
        </Button>
      </div>
      {record.isError && <p className={styles.error}>{record.error.message}</p>}
      {trend.length > 2 && <Sparkline values={trend} width={280} height={56} />}
      {entries.length > 0 ? (
        <ul className={styles.weightList}>
          {entries.slice(0, LIST_SIZE).map((entry) => (
            <li key={entry.id}>
              <span className={styles.muted}>{formatShortDate(`${entry.measuredOn}T12:00:00`)}</span>
              <span>{formatKg(entry.weightKg)}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className={styles.muted}>Записей пока нет — внесите вес, и здесь появится история.</p>
      )}
      {change != null && <p className={styles.muted}>К прошлой записи: {formatChange(Math.round(change * 10) / 10)}</p>}
      <p className="footnote">Взвешивайтесь в одно время, лучше утром до еды — так цифры сравнимы между днями.</p>
    </Card>
  );
}
