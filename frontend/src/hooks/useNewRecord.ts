import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { analyticsApi } from '../api/analytics';
import { authApi } from '../api/auth';
import { useActingClient } from '../trainer/ActingClientProvider';

/** Рекорд "новый" две недели — как бейдж "рекорд" на главной */
const FRESH_DAYS = 14;
const SEEN_KEY = 'vegas.recordsSeen.';

/**
 * localStorage — удобство одного браузера: "этот рекорд я уже видел".
 * Недоступен (приватный режим) — точка просто покажется ещё раз, ничего не сломается.
 */
function readSeen(who: string): string | null {
  try {
    return localStorage.getItem(SEEN_KEY + who);
  } catch {
    return null;
  }
}

function writeSeen(who: string, achievedAt: string) {
  try {
    localStorage.setItem(SEEN_KEY + who, achievedAt);
  } catch {
    // не сохранилось — не страшно
  }
}

/**
 * Точка на вкладке "Рекорды": есть рекорд за последние 14 дней, который человек ещё не видел на странице рекордов.
 * "Видел" запоминаем датой самого свежего рекорда (не текущим временем): новый рекорд позже — точка снова появится.
 * Своя метка у каждого дневника: тренер, открывший дневник клиента, видит точку по рекордам клиента.
 *
 * "Видел" храним ещё и в кэше TanStack Query: вкладка внизу и страница рекордов — разные компоненты,
 * а кэш общий, поэтому точка гаснет сразу, без перезагрузки.
 */
export function useNewRecord(): { fresh: boolean; markSeen: () => void } {
  const queryClient = useQueryClient();
  const { client: acting } = useActingClient();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const who = acting?.id ?? me.data?.id ?? null;

  // самый свежий рекорд: сервер отдаёт рекорды новыми сверху
  const latest = useQuery({
    queryKey: ['records', 'latest'],
    queryFn: () => analyticsApi.records(1),
    enabled: who !== null,
    // рекорды считает аналитика через Kafka уже после "Завершить" — раз в минуту переспрашиваем
    refetchInterval: 60_000,
  });
  const seen = useQuery({
    queryKey: ['recordsSeen', who],
    queryFn: () => readSeen(who!),
    enabled: who !== null,
    staleTime: Infinity,
  });

  const achievedAt = latest.data?.[0]?.achievedAt ?? null;
  const fresh =
    achievedAt !== null &&
    seen.isSuccess &&
    Date.now() - new Date(achievedAt).getTime() < FRESH_DAYS * 24 * 60 * 60 * 1000 &&
    (seen.data === null || new Date(achievedAt) > new Date(seen.data));

  const markSeen = useCallback(() => {
    if (who === null || achievedAt === null) {
      return;
    }
    writeSeen(who, achievedAt);
    queryClient.setQueryData(['recordsSeen', who], achievedAt);
  }, [who, achievedAt, queryClient]);

  return { fresh, markSeen };
}
