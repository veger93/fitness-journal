import { beforeEach, describe, expect, it } from 'vitest';
import { getPendingSets, setPendingOwner, updatePendingSets, type PendingSet } from './pendingSets';

const item = (clientId: string, status: PendingSet['status'] = 'saving'): PendingSet => ({
  clientId,
  workoutId: 'w1',
  workoutExerciseId: 'e1',
  input: { weightKg: 59, reps: 11, durationSec: null, distanceM: null, rpe: null, warmup: false },
  asClient: null,
  status,
});

describe('очередь неподтверждённых подходов', () => {
  beforeEach(() => {
    localStorage.clear();
    setPendingOwner(null);
  });

  it('переживает перезапуск приложения: после входа отдаёт подходы, чтобы их дослать', () => {
    setPendingOwner('me');
    updatePendingSets(() => [item('a', 'failed')]);

    setPendingOwner(null); // «приложение выгрузили»
    const restored = setPendingOwner('me');

    expect(restored.map((p) => p.clientId)).toEqual(['a']);
    expect(restored[0].status).toBe('saving'); // снова отправляем
  });

  it('у каждого пользователя своя очередь', () => {
    setPendingOwner('me');
    updatePendingSets(() => [item('a')]);
    setPendingOwner('other');

    expect(getPendingSets()).toEqual([]);
  });

  it('подтверждённый подход уходит из очереди и из памяти телефона', () => {
    setPendingOwner('me');
    updatePendingSets(() => [item('a')]);
    updatePendingSets((list) => list.filter((p) => p.clientId !== 'a'));

    expect(localStorage.getItem('vegas.pendingSets.me')).toBeNull();
  });
});
