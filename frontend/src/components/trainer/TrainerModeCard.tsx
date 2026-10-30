import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authApi } from '../../api/auth';
import { trainerApi } from '../../api/trainer';
import { Card } from '../ui/Card';
import { Switch } from '../ui/Switch';
import styles from './Trainer.module.css';

/** Профиль: переключатель "Я тренер". Включён — в шапке появляется "Я ▾" с клиентами */
export function TrainerModeCard() {
  const queryClient = useQueryClient();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const toggle = useMutation({
    mutationFn: trainerApi.setTrainerMode,
    onSuccess: (user) => queryClient.setQueryData(['me'], user),
  });

  return (
    <Card>
      <Switch
        label="Я тренер"
        hint="Вести дневники клиентов: записывать им тренировки и смотреть их прогресс"
        checked={me.data?.trainer ?? false}
        onChange={(checked) => toggle.mutate(checked)}
      />
      {toggle.isError && <p className={styles.error}>{toggle.error.message}</p>}
    </Card>
  );
}
