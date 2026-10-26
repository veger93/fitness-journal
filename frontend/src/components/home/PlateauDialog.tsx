import { useQuery } from '@tanstack/react-query';
import { analyticsApi, type ExerciseSummary } from '../../api/analytics';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { PlateauTips } from './PlateauTips';
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
      <PlateauTips />
      <Button fullWidth onClick={onClose}>
        Понятно
      </Button>
    </Dialog>
  );
}
