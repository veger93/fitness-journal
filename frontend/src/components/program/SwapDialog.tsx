import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { programsApi, type ProgramAlternative, type ProgramSlot } from '../../api/programs';
import { workoutsApi, type CatalogExercise } from '../../api/workouts';
import { formatSet, formatShortDate } from '../../lib/format';
import { ExerciseImage } from '../ExerciseImage';
import { ExerciseInfo } from '../ExerciseInfo';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import styles from './SwapDialog.module.css';

/** Вторая строка варианта: что человек уже делал — чтобы выбрать знакомое и сразу взять рабочий вес */
export function alternativeText(a: ProgramAlternative): string {
  let text: string;
  if (!a.familiar) {
    text = 'ещё не делали';
  } else {
    const result = formatSet(a.lastWeightKg, a.lastReps);
    const when = a.lastPerformedAt ? formatShortDate(a.lastPerformedAt) : null;
    text = ['уже делали', [result, when].filter(Boolean).join(', ')].filter(Boolean).join(': ');
  }
  // повтор между днями не запрещаем (дома вариантов мало), но честно предупреждаем
  return a.usedInDay ? `${text} · уже есть в «${a.usedInDay}»` : text;
}

/**
 * Окно «Заменить упражнение» (макет R3, правило П14): только то же семейство движения, один раз на место,
 * замена остаётся в программе и в комплексе дня. «Больше не предлагать» — прежнее уходит в исключения.
 */
export function SwapDialog({ slot, onClose }: { slot: ProgramSlot; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState<string | null>(null);
  const [excludeOld, setExcludeOld] = useState(false);
  // открытая карточка «как выполнять»; «←» возвращает к списку, выбор сохраняется
  const [info, setInfo] = useState<CatalogExercise | null>(null);
  // каталог — тот же кэш, что у окна «Добавить упражнение»: там мышцы и картинки для карточки
  const catalog = useQuery({ queryKey: ['exercises', ''], queryFn: () => workoutsApi.exercises('') });
  // staleTime 0: «что уже делали» меняется после каждой тренировки — берём свежее при каждом открытии
  const alternatives = useQuery({
    queryKey: ['program', 'alternatives', slot.id],
    queryFn: () => programsApi.alternatives(slot.id),
    staleTime: 0,
  });
  const selected = chosen ?? alternatives.data?.[0]?.exerciseId ?? null;
  const oldName = slot.exerciseName.toLowerCase();

  const swap = useMutation({
    mutationFn: () => programsApi.swap(slot.id, selected!, excludeOld),
    onSuccess: (program) => {
      queryClient.setQueryData(['program'], program);
      // упражнение поменялось и в комплексе дня — его видят «Мои комплексы», план и главная
      void queryClient.invalidateQueries({ queryKey: ['templates'] });
      void queryClient.invalidateQueries({ queryKey: ['plan'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      if (excludeOld) {
        void queryClient.invalidateQueries({ queryKey: ['program', 'exclusions'] });
      }
      onClose();
    },
  });

  if (info) {
    return (
      <Dialog open onClose={onClose} onBack={() => setInfo(null)} title={info.name}>
        <ExerciseInfo exercise={info} />
        <Button
          fullWidth
          onClick={() => {
            setChosen(info.id);
            setInfo(null);
          }}
        >
          Выбрать для замены
        </Button>
      </Dialog>
    );
  }

  return (
    <Dialog open onClose={onClose} title={`Заменить: ${oldName}`}>
      <div className={styles.body}>
        <p className={styles.muted}>
          Только упражнения на ту же работу мышц. Замена останется в программе. Новое упражнение начните с лёгкого веса,
          «i» — как выполнять.
        </p>

        {alternatives.isPending && <p className={styles.muted}>Подбираем варианты…</p>}
        {alternatives.isError && <p className={styles.error}>{alternatives.error.message}</p>}
        {alternatives.data?.length === 0 && (
          <p className={styles.notice}>
            Подходящей замены нет: в каталоге больше нет упражнений на эту работу мышц для вашего места и ограничений.
          </p>
        )}

        {alternatives.data && alternatives.data.length > 0 && (
          <>
            <fieldset className={styles.list}>
              <legend className="visually-hidden">Чем заменить</legend>
              {alternatives.data.map((a) => {
                const card = catalog.data?.find((e) => e.id === a.exerciseId);
                return (
                  // (i) — отдельная кнопка рядом, а не внутри label: иначе нажатие на неё выбирало бы вариант
                  <div key={a.exerciseId} className={styles.row}>
                    <label className={styles.option}>
                      <input
                        type="radio"
                        name="swap"
                        value={a.exerciseId}
                        checked={selected === a.exerciseId}
                        onChange={() => setChosen(a.exerciseId)}
                      />
                      <ExerciseImage imageKey={a.imageKey} />
                      <span className={styles.optionBody}>
                        <span className={styles.optionTitle}>{a.name}</span>
                        <span className={styles.muted}>{alternativeText(a)}</span>
                      </span>
                    </label>
                    <button
                      type="button"
                      className={styles.infoButton}
                      aria-label={`Как выполнять: ${a.name}`}
                      disabled={!card}
                      onClick={() => card && setInfo(card)}
                    >
                      i
                    </button>
                  </div>
                );
              })}
            </fieldset>

            <label className={styles.check}>
              <input type="checkbox" checked={excludeOld} onChange={(e) => setExcludeOld(e.target.checked)} />
              <span>Больше не предлагать: {oldName}</span>
            </label>

            <div className={styles.notice}>
              <strong>Заменить можно один раз</strong>
              <span className={styles.muted}>
                Прогресс видно, когда упражнение повторяется неделями. История прежнего упражнения сохранится.
              </span>
            </div>
          </>
        )}

        {swap.isError && <p className={styles.error}>{swap.error.message}</p>}
        <div className={styles.actions}>
          {alternatives.data && alternatives.data.length > 0 && (
            <Button fullWidth disabled={!selected || swap.isPending} onClick={() => swap.mutate()}>
              {swap.isPending ? 'Заменяем…' : 'Заменить'}
            </Button>
          )}
          <Button variant="secondary" fullWidth onClick={onClose}>
            Отмена
          </Button>
        </div>
      </div>
    </Dialog>
  );
}
