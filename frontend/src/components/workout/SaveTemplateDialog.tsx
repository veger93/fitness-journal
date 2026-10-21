import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { workoutsApi } from '../../api/workouts';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { TextField } from '../ui/TextField';
import styles from './Workout.module.css';

interface SaveTemplateDialogProps {
  exerciseIds: string[];
  onClose: () => void;
}

/** "Сохранить как комплекс": упражнения тренировки в том же порядке — в "Мои комплексы". */
export function SaveTemplateDialog({ exerciseIds, onClose }: SaveTemplateDialogProps) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const save = useMutation({
    mutationFn: () => workoutsApi.createTemplate(name.trim(), exerciseIds),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['templates'] }),
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (name.trim()) {
      save.mutate();
    }
  };

  return (
    <Dialog open onClose={onClose} title="Сохранить как комплекс">
      {save.isSuccess ? (
        <>
          <p className={styles.center}>Готово — комплекс «{name.trim()}» теперь в «Мои комплексы».</p>
          <Button fullWidth onClick={onClose}>
            Хорошо
          </Button>
        </>
      ) : (
        <form className={styles.form} onSubmit={submit}>
          <TextField
            label="Название"
            placeholder="например, Верх · понедельник"
            maxLength={100}
            required
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <p className={styles.muted}>Упражнений: {exerciseIds.length}. Порядок — как в тренировке.</p>
          {save.isError && (
            <p className={styles.error} role="alert">
              {save.error.message}
            </p>
          )}
          <Button type="submit" fullWidth disabled={save.isPending || !name.trim()}>
            Сохранить
          </Button>
          <Button variant="secondary" fullWidth onClick={onClose}>
            Отмена
          </Button>
        </form>
      )}
    </Dialog>
  );
}
