import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { trainerApi, type Client } from '../../api/trainer';
import type { ExperienceLevel } from '../../api/users';
import { CLIENTS_KEY } from '../../hooks/useTrainer';
import { Button } from '../ui/Button';
import { Chip } from '../ui/Chip';
import { TextField } from '../ui/TextField';
import styles from './Trainer.module.css';

const LEVELS: { value: ExperienceLevel; label: string }[] = [
  { value: 'BEGINNER', label: 'Новичок' },
  { value: 'INTERMEDIATE', label: 'Средний' },
  { value: 'ADVANCED', label: 'Опытный' },
];

/** "4,5" и "4.5" — одно и то же; пусто -> null */
function toNumber(value: string): number | null {
  const n = Number(value.replace(',', '.'));
  return value.trim() === '' || Number.isNaN(n) ? null : n;
}

/**
 * Новый клиент. Обязательно только имя: это данные человека, который ещё не давал согласия (docs/trainer.md).
 * Остальное свёрнуто в "Ещё данные".
 */
export function NewClientForm({ onCreated }: { onCreated: (client: Client) => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [more, setMore] = useState(false);
  const [weight, setWeight] = useState('');
  const [height, setHeight] = useState('');
  const [level, setLevel] = useState<ExperienceLevel | null>(null);
  const [phone, setPhone] = useState('');

  const create = useMutation({
    mutationFn: trainerApi.createClient,
    onSuccess: (client) => {
      queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
      onCreated(client);
    },
  });

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate({
      displayName: name.trim(),
      phone: phone.trim() || null,
      heightCm: toNumber(height),
      experienceLevel: level,
      weightKg: toNumber(weight),
    });
  };

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <TextField label="Имя*" value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />

      <button type="button" className={styles.more} aria-expanded={more} onClick={() => setMore(!more)}>
        Ещё данные (по желанию) <span aria-hidden="true">{more ? '▴' : '▾'}</span>
      </button>
      {more && (
        <div className={styles.form}>
          <TextField label="Вес, кг" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} />
          <TextField label="Рост, см" inputMode="numeric" value={height} onChange={(e) => setHeight(e.target.value)} />
          <div role="group" aria-label="Уровень подготовки" className={styles.chips}>
            {LEVELS.map((option) => (
              <Chip
                key={option.value}
                selected={level === option.value}
                // повторный тап снимает выбор: поле необязательное
                onClick={() => setLevel(level === option.value ? null : option.value)}
              >
                {option.label}
              </Chip>
            ))}
          </div>
          <TextField
            label="Телефон"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            maxLength={30}
          />
        </div>
      )}

      <p className="footnote">
        Клиенту не нужно регистрироваться — дневник ведёте вы. Позже сможете передать ему аккаунт со всей историей.
      </p>
      {create.isError && <p className={styles.error}>{create.error.message}</p>}
      <Button type="submit" variant="accent" fullWidth disabled={create.isPending || name.trim() === ''}>
        Добавить клиента
      </Button>
    </form>
  );
}
