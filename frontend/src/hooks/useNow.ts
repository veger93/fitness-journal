import { useEffect, useState } from 'react';

/** Текущее время, обновляется раз в intervalMs — для таймера тренировки. */
export function useNow(intervalMs = 1000): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), intervalMs);
    return () => window.clearInterval(id); // уходим со страницы — таймер останавливаем
  }, [intervalMs]);
  return now;
}
