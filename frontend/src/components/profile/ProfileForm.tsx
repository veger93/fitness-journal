import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { usersApi, type ExperienceLevel, type Gender, type Profile } from '../../api/users';
import { Button } from '../ui/Button';
import { ChoiceCards } from '../ui/ChoiceCards';
import { Stepper } from '../ui/Stepper';
import { TextField } from '../ui/TextField';
import styles from './Profile.module.css';

const GENDERS: { value: Gender; title: string }[] = [
  { value: 'MALE', title: 'Мужской' },
  { value: 'FEMALE', title: 'Женский' },
];

const LEVELS: { value: ExperienceLevel; title: string; description: string }[] = [
  { value: 'BEGINNER', title: 'Новичок', description: 'тренируюсь меньше полугода' },
  { value: 'INTERMEDIATE', title: 'Средний', description: 'от полугода до 2 лет' },
  { value: 'ADVANCED', title: 'Опытный', description: 'больше 2 лет' },
];

/** Те же границы, что проверяет user-service: возраст от 10 до 100 лет. Повторяем на фронте, чтобы не ждать ответа сервера. */
const MIN_AGE = 10;
const MAX_AGE = 100;

/**
 * Физические данные — одна форма на два места: онбординг после регистрации и раздел в профиле.
 * Пол и уровень в онбординге не выбраны заранее: подставленное за человека значение он может не заметить.
 */
export function ProfileForm({
  profile,
  submitLabel,
  withWeight = true,
  onSaved,
}: {
  profile: Profile | undefined;
  submitLabel: string;
  /**
   * В профиле вес записывается отдельной карточкой "Вес тела". Там форма его не показывает и отправляет
   * текущий вес из профиля — иначе сохранение формы перезаписало бы только что внесённый вес старым значением.
   */
  withWeight?: boolean;
  onSaved?: () => void;
}) {
  const queryClient = useQueryClient();
  const [gender, setGender] = useState<Gender | null>(profile?.gender ?? null);
  const [level, setLevel] = useState<ExperienceLevel | null>(profile?.experienceLevel ?? null);
  const [birthYear, setBirthYear] = useState(profile?.birthYear ? String(profile.birthYear) : '');
  const [heightCm, setHeightCm] = useState(profile?.heightCm ?? 175);
  const [weightKg, setWeightKg] = useState(profile?.currentWeightKg ?? 75);
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: usersApi.updateProfile,
    onSuccess: (saved) => {
      // ответ сервера — уже свежий профиль: кладём в кэш сразу, без лишнего GET
      queryClient.setQueryData(['profile'], saved);
      queryClient.invalidateQueries({ queryKey: ['weight'] });
      // уровень меняет "норму" на графиках. Аналитика узнаёт о нём через Kafka за пару секунд,
      // поэтому графики перезапросятся при следующем открытии
      queryClient.invalidateQueries({ queryKey: ['progress'] });
      onSaved?.();
    },
    onError: (e) => setError(e.message),
  });

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    const year = Number(birthYear);
    const thisYear = new Date().getFullYear();
    if (!gender || !level) {
      setError('Выберите пол и уровень подготовки.');
      return;
    }
    if (!Number.isInteger(year) || thisYear - year < MIN_AGE || thisYear - year > MAX_AGE) {
      setError(`Год рождения — от ${thisYear - MAX_AGE} до ${thisYear - MIN_AGE}.`);
      return;
    }
    save.mutate({
      gender,
      experienceLevel: level,
      birthYear: year,
      heightCm,
      weightKg: withWeight ? weightKg : (profile?.currentWeightKg ?? weightKg),
    });
  };

  return (
    <form className={styles.form} onSubmit={onSubmit} noValidate>
      <ChoiceCards legend="Пол" options={GENDERS} value={gender} onChange={setGender} />
      <ChoiceCards legend="Уровень подготовки*" options={LEVELS} value={level} onChange={setLevel} />
      <TextField
        label="Год рождения"
        type="number"
        inputMode="numeric"
        placeholder="например, 1993"
        value={birthYear}
        onChange={(e) => setBirthYear(e.target.value)}
      />
      <div className={styles.steppers}>
        <Stepper label="Рост, см" value={heightCm} onChange={setHeightCm} step={1} min={100} max={250} />
        {withWeight && (
          <Stepper
            label="Вес, кг"
            value={weightKg}
            onChange={setWeightKg}
            step={0.5}
            min={20.5}
            max={399.5}
            inputMode="decimal"
          />
        )}
      </div>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <Button type="submit" disabled={save.isPending}>
        {save.isPending ? 'Сохраняем…' : submitLabel}
      </Button>
      <p className="footnote">
        * Уровень задаёт обычный темп роста силы — пунктир «норма для твоего уровня» на графиках. Пол и вес нужны для
        подсказок по весам.
      </p>
    </form>
  );
}
