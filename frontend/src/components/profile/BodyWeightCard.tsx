import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { trainerApi } from '../../api/trainer';
import { localDate, usersApi } from '../../api/users';
import { CLIENTS_KEY } from '../../hooks/useTrainer';
import { formatChange, formatKg, formatShortDate } from '../../lib/format';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Sparkline } from '../ui/Sparkline';
import { Stepper } from '../ui/Stepper';
import styles from './Profile.module.css';

/** Сколько последних записей показать списком (на графике — больше) */
const LIST_SIZE = 5;
const CHART_SIZE = 20;

/**
 * Вес тела: записать сегодняшний и посмотреть, как он менялся.
 * client — карточка в профиле клиента у тренера: те же график и список, данные клиента;
 * записывать можно только клиенту без аккаунта (managed), со своим аккаунтом он взвешивается сам.
 */
export function BodyWeightCard({
  currentWeightKg,
  client,
}: {
  currentWeightKg: number | null;
  client?: { id: string; managed: boolean };
}) {
  const queryClient = useQueryClient();
  const historyKey = client ? ['trainer', 'client-weight', client.id] : ['weight'];
  const history = useQuery({
    queryKey: historyKey,
    queryFn: client ? () => trainerApi.clientWeight(client.id) : usersApi.weightHistory,
  });
  const [weight, setWeight] = useState(currentWeightKg ?? 75);
  const canRecord = !client || client.managed;

  const record = useMutation({
    mutationFn: () =>
      client
        ? trainerApi.recordClientWeight(client.id, localDate(), weight)
        : usersApi.recordWeight(localDate(), weight),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: historyKey });
      // "текущий вес" = последняя запись: в своём профиле и в карточке клиента
      if (client) {
        queryClient.invalidateQueries({ queryKey: ['trainer', 'client', client.id] });
        queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
      } else {
        queryClient.invalidateQueries({ queryKey: ['profile'] });
      }
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
      {!canRecord && <p className={styles.muted}>Вес клиент записывает сам в своём аккаунте.</p>}
      {canRecord && (
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
      )}
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
      {canRecord && (
        <p className="footnote">Взвешивайтесь в одно время, лучше утром до еды — так цифры сравнимы между днями.</p>
      )}
    </Card>
  );
}
