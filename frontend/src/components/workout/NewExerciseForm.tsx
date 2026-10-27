import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import {
  workoutsApi,
  type BodyPart,
  type CatalogExercise,
  type Equipment,
  type TrackingType,
} from '../../api/workouts';
import { BODY_PART_NAMES } from '../../lib/plan';
import { Button } from '../ui/Button';
import { ChoiceCards } from '../ui/ChoiceCards';
import { Chip } from '../ui/Chip';
import { TextField } from '../ui/TextField';
import styles from './Workout.module.css';

const TRACKING: { value: TrackingType; title: string; description: string }[] = [
  { value: 'WEIGHT_REPS', title: 'Вес × повторы', description: 'жим, тяга, выпады' },
  { value: 'TIME', title: 'Время', description: 'планка, вис' },
  { value: 'DISTANCE', title: 'Дистанция', description: 'бег, гребля, велосипед' },
];

const EQUIPMENT: { value: Equipment; label: string }[] = [
  { value: 'BARBELL', label: 'Штанга' },
  { value: 'DUMBBELL', label: 'Гантели' },
  { value: 'MACHINE', label: 'Тренажёр' },
  { value: 'BODYWEIGHT', label: 'Свой вес' },
];

const BODY_PARTS = Object.entries(BODY_PART_NAMES) as [BodyPart, string][];

/**
 * Своё упражнение: название, что записываем, группа мышц, снаряд.
 * Снаряд спрашиваем только для "вес × повторы": от него зависит стартовый вес в подсказке подхода.
 */
export function NewExerciseForm({
  initialName,
  onCreated,
  onCancel,
}: {
  initialName: string;
  onCreated: (exercise: CatalogExercise) => void;
  onCancel: () => void;
}) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(initialName);
  const [tracking, setTracking] = useState<TrackingType | null>(null);
  const [bodyPart, setBodyPart] = useState<BodyPart | null>(null);
  const [equipment, setEquipment] = useState<Equipment | null>(null);
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: workoutsApi.createExercise,
    onSuccess: (created) => {
      // каталог в поиске должен знать о новом упражнении
      queryClient.invalidateQueries({ queryKey: ['exercises'] });
      onCreated(created);
    },
    // 409 с сервера: "уже есть в каталоге" / "у вас уже есть" — показываем как есть
    onError: (e) => setError(e.message),
  });

  const weighted = tracking === 'WEIGHT_REPS';

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!name.trim() || !tracking || !bodyPart || (weighted && !equipment)) {
      setError(weighted ? 'Заполните название, группу мышц и снаряд.' : 'Заполните название и группу мышц.');
      return;
    }
    create.mutate({
      name: name.trim(),
      trackingType: tracking,
      bodyPart,
      // на время и дистанцию снаряд не влияет — записываем "свой вес"
      equipment: weighted ? equipment! : 'BODYWEIGHT',
      icon: null,
      note: null,
    });
  };

  return (
    <form className={styles.newExercise} onSubmit={onSubmit} noValidate>
      <TextField
        label="Название"
        value={name}
        maxLength={100}
        placeholder="например, жим Арнольда"
        onChange={(e) => setName(e.target.value)}
        autoFocus
      />
      <ChoiceCards legend="Что записываем" options={TRACKING} value={tracking} onChange={setTracking} />
      <fieldset className={styles.choice}>
        <legend className={styles.legend}>Группа мышц</legend>
        <div className={styles.chips}>
          {BODY_PARTS.map(([value, label]) => (
            <Chip key={value} selected={bodyPart === value} onClick={() => setBodyPart(value)}>
              {label}
            </Chip>
          ))}
        </div>
      </fieldset>
      {weighted && (
        <fieldset className={styles.choice}>
          <legend className={styles.legend}>Снаряд*</legend>
          <div className={styles.chips}>
            {EQUIPMENT.map((item) => (
              <Chip key={item.value} selected={equipment === item.value} onClick={() => setEquipment(item.value)}>
                {item.label}
              </Chip>
            ))}
          </div>
        </fieldset>
      )}
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <div className={styles.formActions}>
        <Button type="submit" disabled={create.isPending}>
          {create.isPending ? 'Создаём…' : 'Создать и добавить'}
        </Button>
        <Button type="button" variant="secondary" onClick={onCancel}>
          Назад к поиску
        </Button>
      </div>
      {weighted && <p className="footnote">* От снаряда зависит стартовый вес в подсказке первого подхода.</p>}
    </form>
  );
}
