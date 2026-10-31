import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { trainerApi, type Client } from '../../api/trainer';
import type { ExperienceLevel } from '../../api/users';
import { CLIENTS_KEY } from '../../hooks/useTrainer';
import { CLIENT_LEVELS, toNumber } from '../../lib/trainer';
import { useActingClient } from '../../trainer/ActingClientProvider';
import { BodyWeightCard } from '../profile/BodyWeightCard';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Chip } from '../ui/Chip';
import { TextField } from '../ui/TextField';
import { HandoverCard } from './HandoverCard';
import { RemoveClientCard } from './RemoveClientCard';
import styles from './Trainer.module.css';

const LEVEL_NAMES = { BEGINNER: 'новичок', INTERMEDIATE: 'средний', ADVANCED: 'опытный' } as const;

/**
 * "Профиль" в режиме клиента: карточка клиента из записной книжки тренера (имя, телефон, рост, уровень)
 * и вес тела с графиком — как в своём профиле. У клиента без аккаунта тренер правит всё; со своим аккаунтом —
 * только телефон (это записная книжка тренера), остальное клиент меняет сам.
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
        <ClientDetailsForm
          key={`${data.id}:${data.heightCm}:${data.experienceLevel}`}
          client={data}
          onSaved={(saved) => {
            queryClient.setQueryData(['trainer', 'client', clientId], saved);
            queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
            actAs({ id: saved.id, displayName: saved.displayName });
          }}
        />
      </Card>
      <BodyWeightCard
        key={data.id}
        currentWeightKg={data.currentWeightKg}
        client={{ id: data.id, managed: data.managed }}
      />
      {/* передать можно только ведомого: у клиента со своим аккаунтом вход уже есть */}
      {data.managed && <HandoverCard clientId={data.id} clientName={data.displayName} />}
      <RemoveClientCard clientId={data.id} clientName={data.displayName} managed={data.managed} />
    </>
  );
}

function ClientDetailsForm({ client, onSaved }: { client: Client; onSaved: (client: Client) => void }) {
  const { managed } = client;
  const [name, setName] = useState(client.displayName);
  const [phone, setPhone] = useState(client.phone ?? '');
  const [height, setHeight] = useState(client.heightCm == null ? '' : String(client.heightCm));
  const [level, setLevel] = useState<ExperienceLevel | null>(client.experienceLevel);
  const save = useMutation({
    mutationFn: () =>
      trainerApi.updateClient(client.id, {
        displayName: name.trim(),
        phone: phone.trim() || null,
        heightCm: toNumber(height),
        experienceLevel: level,
      }),
    onSuccess: onSaved,
  });
  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const changed =
    name.trim() !== client.displayName ||
    phone.trim() !== (client.phone ?? '') ||
    toNumber(height) !== client.heightCm ||
    level !== client.experienceLevel;
  const ownAccountHint = managed ? undefined : 'Клиент меняет сам в своём аккаунте';

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <TextField
        label="Имя"
        value={name}
        onChange={(e) => setName(e.target.value)}
        disabled={!managed}
        hint={ownAccountHint}
        required
      />
      <TextField label="Телефон" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={30} />
      <TextField
        label="Рост, см"
        inputMode="numeric"
        value={height}
        onChange={(e) => setHeight(e.target.value)}
        disabled={!managed}
        hint={ownAccountHint}
      />
      <p className={styles.muted}>Уровень подготовки</p>
      <div role="group" aria-label="Уровень подготовки" className={styles.chips}>
        {CLIENT_LEVELS.map((option) => (
          <Chip
            key={option.value}
            selected={level === option.value}
            disabled={!managed}
            // повторный тап снимает выбор: поле необязательное
            onClick={() => setLevel(level === option.value ? null : option.value)}
          >
            {option.label}
          </Chip>
        ))}
      </div>
      {save.isError && <p className={styles.error}>{save.error.message}</p>}
      <Button type="submit" variant="secondary" disabled={!changed || save.isPending || name.trim() === ''}>
        Сохранить
      </Button>
    </form>
  );
}
