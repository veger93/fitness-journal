import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { trainerApi } from '../../api/trainer';
import { formatShortDate } from '../../lib/format';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import styles from './Trainer.module.css';

const MY_TRAINER_KEY = ['me', 'trainer'] as const;

/**
 * Профиль клиента: "Мой тренер". Отключить может только сам клиент — после этого тренер
 * больше не видит и не записывает его тренировки (данные остаются у клиента).
 * Подтверждение — второй кнопкой, а не окном браузера: confirm() на телефоне выглядит чужеродно.
 */
export function MyTrainerCard() {
  const queryClient = useQueryClient();
  const trainer = useQuery({ queryKey: MY_TRAINER_KEY, queryFn: trainerApi.myTrainer });
  const [confirming, setConfirming] = useState(false);
  const leave = useMutation({
    mutationFn: trainerApi.leaveTrainer,
    onSuccess: () => queryClient.setQueryData(MY_TRAINER_KEY, null),
  });

  if (!trainer.data) {
    return null; // тренера нет — карточку не показываем
  }

  return (
    <Card>
      <h2 className={styles.cardTitle}>Мой тренер</h2>
      <p>
        {trainer.data.trainerName ?? 'Тренер'}{' '}
        <span className={styles.muted}>· с {formatShortDate(trainer.data.since)}</span>
      </p>
      <p className={styles.muted}>Тренер видит ваши тренировки и может записывать подходы за вас.</p>
      {leave.isError && <p className={styles.error}>{leave.error.message}</p>}
      {confirming ? (
        <div className={styles.actions}>
          <Button variant="accent" onClick={() => leave.mutate()} disabled={leave.isPending}>
            Да, отключить
          </Button>
          <Button variant="secondary" onClick={() => setConfirming(false)}>
            Отмена
          </Button>
        </div>
      ) : (
        <Button variant="secondary" onClick={() => setConfirming(true)}>
          Отключить тренера
        </Button>
      )}
    </Card>
  );
}
