import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { trainerApi } from '../../api/trainer';
import { CLIENTS_KEY } from '../../hooks/useTrainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { TextField } from '../ui/TextField';
import { HandoverCard } from './HandoverCard';
import { RemoveClientCard } from './RemoveClientCard';
import styles from './Trainer.module.css';

const LEVEL_NAMES = { BEGINNER: 'новичок', INTERMEDIATE: 'средний', ADVANCED: 'опытный' } as const;

/**
 * "Профиль" в режиме клиента: карточка клиента из записной книжки тренера (имя, телефон, данные).
 * Свой профиль, тема и выход — у тренера, они здесь не показываются.
 */
export function ClientProfileCard({ clientId }: { clientId: string }) {
  const queryClient = useQueryClient();
  const { actAs } = useActingClient();
  const client = useQuery({ queryKey: ['trainer', 'client', clientId], queryFn: () => trainerApi.client(clientId) });

  if (!client.data) {
    return <p className={styles.muted}>{client.isError ? client.error.message : 'Загружаем…'}</p>;
  }
  const data = client.data;
  const facts = [
    data.currentWeightKg && `${String(data.currentWeightKg).replace('.', ',')} кг`,
    data.heightCm && `${data.heightCm} см`,
    data.experienceLevel && LEVEL_NAMES[data.experienceLevel],
  ].filter(Boolean);

  return (
    <>
      <Card>
        <p className={styles.muted}>{data.managed ? 'Дневник ведёте вы' : 'У клиента свой аккаунт'}</p>
        {facts.length > 0 && <p>{facts.join(' · ')}</p>}
        <ClientContactsForm
          key={data.id}
          clientId={data.id}
          managed={data.managed}
          name={data.displayName}
          phone={data.phone ?? ''}
          onSaved={(saved) => {
            queryClient.setQueryData(['trainer', 'client', clientId], saved);
            queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
            actAs({ id: saved.id, displayName: saved.displayName });
          }}
        />
      </Card>
      {/* передать можно только ведомого: у клиента со своим аккаунтом вход уже есть */}
      {data.managed && <HandoverCard clientId={data.id} clientName={data.displayName} />}
      <RemoveClientCard clientId={data.id} clientName={data.displayName} managed={data.managed} />
    </>
  );
}

function ClientContactsForm({
  clientId,
  managed,
  name: initialName,
  phone: initialPhone,
  onSaved,
}: {
  clientId: string;
  managed: boolean;
  name: string;
  phone: string;
  onSaved: (client: Awaited<ReturnType<typeof trainerApi.client>>) => void;
}) {
  const [name, setName] = useState(initialName);
  const [phone, setPhone] = useState(initialPhone);
  const save = useMutation({
    mutationFn: () => trainerApi.updateClient(clientId, { displayName: name.trim(), phone: phone.trim() || null }),
    onSuccess: onSaved,
  });
  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const changed = name.trim() !== initialName || phone.trim() !== initialPhone;

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <TextField
        label="Имя"
        value={name}
        onChange={(e) => setName(e.target.value)}
        disabled={!managed}
        hint={managed ? undefined : 'Имя клиент меняет сам в своём аккаунте'}
        required
      />
      <TextField label="Телефон" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={30} />
      {save.isError && <p className={styles.error}>{save.error.message}</p>}
      <Button type="submit" variant="secondary" disabled={!changed || save.isPending || name.trim() === ''}>
        Сохранить
      </Button>
    </form>
  );
}
