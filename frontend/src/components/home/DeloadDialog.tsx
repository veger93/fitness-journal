import type { ExerciseSummary } from '../../api/analytics';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { DeloadPlanContent } from './DeloadPlanContent';

/** План разгрузочной недели окном — с главной. На странице упражнения тот же план показан блоком. */
export function DeloadDialog({ exercise, onClose }: { exercise: ExerciseSummary; onClose: () => void }) {
  return (
    <Dialog open onClose={onClose} title="План разгрузки" subtitle={exercise.exerciseName}>
      <DeloadPlanContent exerciseId={exercise.exerciseId} />
      <Button fullWidth onClick={onClose}>
        Понятно
      </Button>
    </Dialog>
  );
}
