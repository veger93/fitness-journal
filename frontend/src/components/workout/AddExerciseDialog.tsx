import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { analyticsApi } from '../../api/analytics';
import { workoutsApi, type BodyPart, type CatalogExercise, type Equipment } from '../../api/workouts';
import {
  BODY_PART_TILES,
  EQUIPMENT_NAMES,
  countByBodyPart,
  equipmentIn,
  exercisesCount,
  filterCatalog,
  recentExercises,
} from '../../lib/catalog';
import { musclesText } from '../../lib/exercise';
import { BODY_PART_NAMES } from '../../lib/plan';
import { ExerciseImage } from '../ExerciseImage';
import { ExerciseInfo } from '../ExerciseInfo';
import { Button } from '../ui/Button';
import { Chip } from '../ui/Chip';
import { Dialog } from '../ui/Dialog';
import { TextField } from '../ui/TextField';
import { NewExerciseForm } from './NewExerciseForm';
import styles from './Workout.module.css';

interface AddExerciseDialogProps {
  /** уже есть в тренировке — помечаем, чтобы не добавить дважды по ошибке */
  existingIds: string[];
  adding: boolean;
  onAdd: (exerciseId: string) => void;
  onClose: () => void;
}

/**
 * Выбор упражнения в два шага: плитки частей тела → список внутри с чипами снаряда.
 * Сверху — недавние упражнения (добавить в одно нажатие) и поиск сразу по всему каталогу.
 * По "i" — карточка упражнения с картинками и техникой. Нет нужного — создаём своё.
 *
 * Каталог грузим один раз целиком и фильтруем в браузере (lib/catalog.ts): упражнений десятки,
 * а переключение плиток и чипов так мгновенное.
 */
export function AddExerciseDialog({ existingIds, adding, onAdd, onClose }: AddExerciseDialogProps) {
  const [search, setSearch] = useState('');
  const [bodyPart, setBodyPart] = useState<BodyPart | null>(null);
  const [equipment, setEquipment] = useState<Equipment | null>(null);
  const [creating, setCreating] = useState(false);
  // открытая карточка; фильтры при этом не сбрасываем — "←" вернёт туда же
  const [info, setInfo] = useState<CatalogExercise | null>(null);

  const catalog = useQuery({ queryKey: ['exercises', ''], queryFn: () => workoutsApi.exercises('') });
  // недавние берём из сводки главной (тот же кэш): там у каждого упражнения дата последней тренировки
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: analyticsApi.dashboard });

  if (creating) {
    return (
      <Dialog open onClose={onClose} onBack={() => setCreating(false)} title="Своё упражнение">
        <NewExerciseForm
          initialName={search.trim()}
          onCreated={(exercise) => onAdd(exercise.id)}
          onCancel={() => setCreating(false)}
        />
      </Dialog>
    );
  }

  if (info) {
    const already = existingIds.includes(info.id);
    return (
      <Dialog open onClose={onClose} onBack={() => setInfo(null)} title={info.name}>
        <ExerciseInfo exercise={info} />
        <Button fullWidth disabled={adding || already} onClick={() => onAdd(info.id)}>
          {already ? 'Уже в тренировке' : 'Добавить в тренировку'}
        </Button>
      </Dialog>
    );
  }

  const all = catalog.data ?? [];
  const searching = search.trim() !== '';
  const inBodyPart = bodyPart ? all.filter((e) => e.bodyPart === bodyPart) : [];
  const list = filterCatalog(all, { search, bodyPart, equipment });
  const back = () => {
    setBodyPart(null);
    setEquipment(null);
  };

  const row = (exercise: CatalogExercise) => (
    <CatalogRow
      key={exercise.id}
      exercise={exercise}
      already={existingIds.includes(exercise.id)}
      adding={adding}
      onAdd={onAdd}
      onInfo={setInfo}
    />
  );

  return (
    <Dialog
      open
      onClose={onClose}
      onBack={bodyPart && !searching ? back : undefined}
      title={bodyPart && !searching ? BODY_PART_NAMES[bodyPart] : 'Добавить упражнение'}
    >
      <TextField
        label="Поиск"
        type="search"
        placeholder="например, жим"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      {catalog.isPending && <p className={styles.muted}>Загружаем упражнения…</p>}
      {catalog.isError && <p className={styles.muted}>{catalog.error.message}</p>}

      {catalog.data && searching && (
        <ul className={styles.catalog}>
          {list.map(row)}
          {list.length === 0 && <li className={styles.muted}>Ничего не нашлось</li>}
        </ul>
      )}

      {catalog.data && !searching && !bodyPart && (
        <Categories
          catalog={all}
          recent={dashboard.data ? recentExercises(dashboard.data.exercises, all) : []}
          existingIds={existingIds}
          adding={adding}
          onAdd={onAdd}
          onPick={setBodyPart}
        />
      )}

      {catalog.data && !searching && bodyPart && (
        <>
          <div className={styles.chipRow} role="group" aria-label="Снаряд">
            <Chip selected={equipment === null} onClick={() => setEquipment(null)}>
              Все
            </Chip>
            {equipmentIn(inBodyPart).map((eq) => (
              <Chip key={eq} selected={equipment === eq} onClick={() => setEquipment(eq)}>
                {EQUIPMENT_NAMES[eq]}
              </Chip>
            ))}
          </div>
          <ul className={styles.catalog}>{list.map(row)}</ul>
        </>
      )}

      <button type="button" className={styles.createLink} onClick={() => setCreating(true)}>
        Нет нужного? Создать своё упражнение
      </button>
    </Dialog>
  );
}

/** Первый шаг: недавние (добавить в одно нажатие) и плитки частей тела с числом упражнений. */
function Categories({
  catalog,
  recent,
  existingIds,
  adding,
  onAdd,
  onPick,
}: {
  catalog: CatalogExercise[];
  recent: CatalogExercise[];
  existingIds: string[];
  adding: boolean;
  onAdd: (exerciseId: string) => void;
  onPick: (bodyPart: BodyPart) => void;
}) {
  const counts = countByBodyPart(catalog);
  return (
    <>
      {recent.length > 0 && (
        <section className={styles.catalogSection}>
          <h3 className={styles.sectionLabel}>Недавние</h3>
          <div className={styles.chipRow}>
            {recent.map((exercise) => (
              <Chip
                key={exercise.id}
                disabled={adding || existingIds.includes(exercise.id)}
                onClick={() => onAdd(exercise.id)}
              >
                {exercise.name}
              </Chip>
            ))}
          </div>
        </section>
      )}
      <section className={styles.catalogSection}>
        <h3 className={styles.sectionLabel}>Часть тела</h3>
        <div className={styles.tiles}>
          {BODY_PART_TILES.map(({ bodyPart, imageKey }) => (
            <button key={bodyPart} type="button" className={styles.tile} onClick={() => onPick(bodyPart)}>
              <ExerciseImage imageKey={imageKey} size="large" />
              <span className={styles.tileName}>{BODY_PART_NAMES[bodyPart]}</span>
              <span className={styles.muted}>{exercisesCount(counts[bodyPart])}</span>
            </button>
          ))}
        </div>
      </section>
    </>
  );
}

/** Строка списка — две кнопки рядом: "добавить" и "что это". Кнопку в кнопку вкладывать нельзя */
function CatalogRow({
  exercise,
  already,
  adding,
  onAdd,
  onInfo,
}: {
  exercise: CatalogExercise;
  already: boolean;
  adding: boolean;
  onAdd: (exerciseId: string) => void;
  onInfo: (exercise: CatalogExercise) => void;
}) {
  return (
    <li className={styles.catalogRow}>
      <button
        type="button"
        className={styles.catalogItem}
        disabled={adding || already}
        onClick={() => onAdd(exercise.id)}
      >
        <ExerciseImage imageKey={exercise.imageKey} />
        <span className={styles.catalogText}>
          <span>
            {exercise.name}
            {exercise.custom && <span className={styles.muted}> · своё</span>}
          </span>
          <span className={styles.muted}>{already ? 'уже в тренировке' : musclesText(exercise.muscles, 'PRIMARY')}</span>
        </span>
      </button>
      <button
        type="button"
        className={styles.infoButton}
        aria-label={`Как выполнять: ${exercise.name}`}
        onClick={() => onInfo(exercise)}
      >
        i
      </button>
    </li>
  );
}
