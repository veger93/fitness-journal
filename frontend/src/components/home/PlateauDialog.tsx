import { useQuery } from '@tanstack/react-query';
import { analyticsApi, type ExerciseSummary } from '../../api/analytics';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import styles from './Home.module.css';

/** Советы при плато: общие приёмы + рекомендация системы по этому упражнению. */
export function PlateauDialog({ exercise, onClose }: { exercise: ExerciseSummary; onClose: () => void }) {
  const insights = useQuery({
    queryKey: ['insights', exercise.exerciseId],
    queryFn: () => analyticsApi.insights(exercise.exerciseId),
  });

  return (
    <Dialog open onClose={onClose} title="Что попробовать" subtitle={exercise.exerciseName}>
      {insights.data && <p className={styles.signalText}>{insights.data.recommendation.message}</p>}
      <ul className={styles.list}>
        <li>Поменяйте число повторов: например, 5 вместо 8–10 на ближайшие 2–3 недели.</li>
        <li>Добавьте один подход в этом упражнении.</li>
        <li>Если устали — возьмите неделю полегче и вернитесь отдохнувшим.</li>
      </ul>
      <Button fullWidth onClick={onClose}>
        Понятно
      </Button>
    </Dialog>
  );
}
