import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { trainerApi } from '../../api/trainer';
import { CLIENTS_KEY } from '../../hooks/useTrainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import styles from './Trainer.module.css';

/**
 * Клиент ушёл насовсем. Два разных случая (docs/trainer.md):
 * - без своего аккаунта — удаляем вместе со всеми тренировками: данные больше никому не нужны;
 * - со своим аккаунтом — только отвязываем: аккаунт и история остаются у клиента.
 * Подтверждение — второй кнопкой с понятным последствием, удаление нельзя отменить.
 */
export function RemoveClientCard({
  clientId,
  clientName,
  managed,
}: {
  clientId: string;
  clientName: string;
  managed: boolean;
}) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { actAs } = useActingClient();
  const [confirming, setConfirming] = useState(false);
  const remove = useMutation({
    mutationFn: () => trainerApi.removeClient(clientId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
      actAs(null); // клиента больше нет — возвращаемся в свой дневник
      navigate('/');
    },
  });

  return (
    <Card>
      <h2 className={styles.cardTitle}>{managed ? 'Удалить клиента' : 'Отвязать клиента'}</h2>
      <p className={styles.muted}>
        {managed
          ? `${clientName} и все его тренировки, рекорды и графики будут удалены. Это нельзя отменить. Если он захочет вести дневник сам — сначала передайте ему аккаунт.`
          : `У ${clientName} свой аккаунт — он и вся его история останутся у него. Вы просто перестанете видеть его тренировки.`}
      </p>
      {remove.isError && <p className={styles.error}>{remove.error.message}</p>}
      {confirming ? (
        <div className={styles.actions}>
          <Button variant="accent" onClick={() => remove.mutate()} disabled={remove.isPending}>
            {managed ? 'Да, удалить навсегда' : 'Да, отвязать'}
          </Button>
          <Button variant="secondary" onClick={() => setConfirming(false)}>
            Отмена
          </Button>
        </div>
      ) : (
        <Button variant="secondary" className={styles.danger} onClick={() => setConfirming(true)}>
          {managed ? 'Удалить клиента' : 'Отвязать клиента'}
        </Button>
      )}
    </Card>
  );
}
