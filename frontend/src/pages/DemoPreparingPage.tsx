import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { analyticsApi, type Dashboard } from '../api/analytics';
import { authApi } from '../api/auth';
import { trainerApi, type ClientSignal } from '../api/trainer';
import styles from './DemoPreparingPage.module.css';

/** Дольше ждать нет смысла — откроем главную как есть (данные догрузятся при следующих запросах) */
const GIVE_UP_MS = 40_000;
/** После стольких секунд — "чуть дольше обычного": сервис только что перезапустили */
const SLOW_MS = 15_000;
const STEPS = ['Полгода тренировок', 'Рекорды, плато и перегрузки', 'Отчёт за месяц'];

/** Сколько одинаковых ответов подряд считаем "аналитика досчитала" (опрос раз в секунду) */
const STABLE_POLLS = 3;

/**
 * "Отпечаток" готовности: пока аналитика считает события, он меняется.
 * У демо-тренера — ещё и сигналы клиентов: их тренировки генерируются после его собственных,
 * поэтому одной его сводки мало (иначе клиенты на секунду показали бы "8 нед без тренировок").
 * null — ещё точно не готово.
 */
function fingerprint(d: Dashboard | undefined, signals: ClientSignal[] | undefined, trainer: boolean): string | null {
  if (!d || d.empty) {
    return null;
  }
  const own = `${d.workoutsThisMonth}|${d.exercises.map((e) => e.status).join(',')}`;
  if (!trainer) {
    return own;
  }
  // каждый клиент должен уже иметь тренировки
  if (!signals || signals.length === 0 || signals.some((s) => s.lastWorkoutAt === null)) {
    return null;
  }
  const clients = [...signals]
    .sort((a, b) => a.clientId.localeCompare(b.clientId))
    .map((s) => `${s.signal}:${s.days ?? ''}`)
    .join(',');
  return `${own}#${clients}`;
}

/**
 * "Готовим ваш дневник…" (docs/demo.md): сервер уже создал демо-пользователя, а тренировки генерируются
 * и считаются аналитикой ещё несколько секунд. Опрашиваем сводку раз в секунду: она не пустая и не меняется
 * два опроса подряд — открываем главную. Настоящему пользователю эта страница не нужна — уводим на главную.
 */
export function DemoPreparingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const [startedAt] = useState(() => Date.now());
  const [elapsed, setElapsed] = useState(0);
  const previous = useRef<string | null>(null);
  const sameInRow = useRef(0);
  const dashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: analyticsApi.dashboard,
    refetchInterval: 1000,
    // каждый опрос — свежий ответ, без кэша: ждём, пока аналитика досчитает
    staleTime: 0,
  });
  const trainer = me.data?.trainer ?? false;
  const signals = useQuery({
    queryKey: ['trainer', 'signals'],
    queryFn: trainerApi.clientSignals,
    enabled: trainer,
    refetchInterval: 1000,
    staleTime: 0,
  });

  useEffect(() => {
    const timer = setInterval(() => setElapsed(Date.now() - startedAt), 500);
    return () => clearInterval(timer);
  }, [startedAt]);

  // главная загрузит всё заново (уведомления, список клиентов тренера) — уже готовые данные
  const done = useRef(() => {});
  useEffect(() => {
    done.current = () => {
      queryClient.invalidateQueries();
      navigate('/', { replace: true });
    };
  }, [navigate, queryClient]);

  // сравниваем только НОВЫЕ ответы сервера (dataUpdatedAt), а не каждый тик таймера:
  // иначе один и тот же ответ дважды "совпал бы сам с собой"
  // один шаг проверки — на каждый новый ответ сводки (раз в секунду); сигналы берём последние известные
  const evaluatedAt = useRef(0);
  useEffect(() => {
    if (!me.data || dashboard.dataUpdatedAt === evaluatedAt.current) {
      return;
    }
    evaluatedAt.current = dashboard.dataUpdatedAt;
    const current = fingerprint(dashboard.data, signals.data, trainer);
    sameInRow.current = current !== null && current === previous.current ? sameInRow.current + 1 : 0;
    previous.current = current;
    if (sameInRow.current >= STABLE_POLLS - 1) {
      done.current();
    }
  }, [dashboard.dataUpdatedAt, dashboard.data, signals.data, trainer, me.data]);

  useEffect(() => {
    if (elapsed > GIVE_UP_MS) {
      done.current();
    }
  }, [elapsed]);

  if (me.data && !me.data.demo) {
    return <Navigate to="/" replace />;
  }
  // шаги "загораются" по времени — честнее, чем фальшивый процент: реальная готовность — по сводке выше
  const doneSteps = Math.min(STEPS.length, Math.floor(elapsed / 1500));

  return (
    <main className={styles.page}>
      <div className={styles.box} role="status" aria-live="polite">
        <div className={styles.spinner} aria-hidden="true" />
        <h1 className={styles.title}>Готовим ваш дневник…</h1>
        <ul className={styles.steps}>
          {STEPS.map((step, i) => (
            <li key={step} className={i < doneSteps ? styles.done : undefined}>
              {i < doneSteps ? '✓' : '·'} {step}
            </li>
          ))}
        </ul>
        {elapsed > SLOW_MS && <p className={styles.slow}>Чуть дольше обычного — сервер просыпается. Ещё немного.</p>}
      </div>
    </main>
  );
}
