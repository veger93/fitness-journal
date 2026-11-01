import { useEffect, useId, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import type { DemoScenario } from '../api/auth';
import { useAuth } from '../auth/AuthProvider';
import { clearDemoSession, hasDemoSession } from '../lib/demo';
import { ButtonLink } from '../components/ui/Button';
import { DEMO_REFERENCE, DEMO_STRENGTH, chartPaths } from '../lib/landing';
import { THEME_COLOR } from '../theme/theme';
import styles from './LandingPage.module.css';

/** Бегущая строка: что умеет приложение */
const FEATURES = [
  'Графики силы',
  'Рекорды',
  'Детектор плато',
  'Разгрузка в один клик',
  'Итоги месяца',
  '97 упражнений с техникой',
  'План на неделю',
  'Бег',
];

const POINTS = [
  { icon: '↗', title: 'Прогрессия нагрузки', text: 'когда и на сколько добавлять вес' },
  { icon: '≈', title: 'Детектор плато', text: '3–4 недели без роста — и предложение, что поменять' },
  { icon: '↓', title: 'Разгрузка в один клик', text: 'неделя облегчённых весов, чтобы снова пойти вверх' },
  { icon: '★', title: 'Рекорды и итоги месяца', text: '«было / стало» по каждому упражнению' },
];

/**
 * Страница для гостя на "/" — что за приложение и зачем регистрироваться.
 * Всегда тёмная (data-theme="dark"), независимо от темы в настройках: это "витрина", её стиль фиксирован.
 * Макеты экранов внутри — светлые островки (data-theme="light"), как приложение выглядит по умолчанию.
 */
/**
 * "Посмотреть изнутри" (docs/demo.md, вариант A): две кнопки — демо спортсмена и демо тренера.
 * Отдельный блок с подписью "без регистрации", чтобы не казалось, что кнопки создают аккаунт.
 * После клика — сразу вход в демо и экран "Готовим ваш дневник…", пока сервер генерирует тренировки.
 */
function DemoEntry() {
  const { startDemo } = useAuth();
  const navigate = useNavigate();
  const [pending, setPending] = useState<DemoScenario | null>(null);
  const [error, setError] = useState<string | null>(null);
  // демо закончилось (сутки прошли) — гость вернулся сюда: говорим, что случилось. Метку убираем после показа
  const [ended] = useState(hasDemoSession);
  useEffect(() => {
    if (ended) clearDemoSession();
  }, [ended]);

  const start = async (scenario: DemoScenario) => {
    setPending(scenario);
    setError(null);
    try {
      await startDemo(scenario);
      navigate('/demo', { replace: true });
    } catch (e) {
      // 429 — лимит демо: сервер присылает понятный текст
      setError(e instanceof Error ? e.message : 'Не получилось открыть демо — попробуйте ещё раз');
      setPending(null);
    }
  };

  return (
    <section className={styles.demo} aria-labelledby="demo-title">
      {ended && (
        <p className={styles.demoEnded}>Демо закончилось. Понравилось — заведите свой дневник, это бесплатно.</p>
      )}
      <p id="demo-title" className={styles.demoTitle}>
        Посмотреть изнутри — без регистрации
      </p>
      <div className={styles.demoButtons}>
        <button
          type="button"
          className={styles.demoButton}
          disabled={pending !== null}
          onClick={() => start('ATHLETE')}
        >
          <b>{pending === 'ATHLETE' ? 'Открываем…' : 'Демо спортсмена'}</b>
          <span>рекорды, плато, графики</span>
        </button>
        <button type="button" className={styles.demoButton} disabled={pending !== null} onClick={() => start('COACH')}>
          <b>{pending === 'COACH' ? 'Открываем…' : 'Демо тренера'}</b>
          <span>5 клиентов и подсказки</span>
        </button>
      </div>
      {error && <p className={styles.demoError}>{error}</p>}
      <p className={styles.demoNote}>Готовый дневник с примерами. Можно всё трогать — через сутки он удалится сам.</p>
    </section>
  );
}

export function LandingPage() {
  useDarkBrowserBar();

  return (
    <div data-theme="dark" className={styles.page}>
      <section className={styles.hero}>
        <div className={styles.heroPhoto} role="img" aria-label="Атлет в зале" />
        <div className={styles.glow} aria-hidden="true" />
        <header className={styles.topBar}>
          <span className={styles.logo}>
            <img src="/favicon.svg" alt="" width={32} height={32} />
            VEGAS
          </span>
          <Link to="/login" className={styles.login}>
            Войти
          </Link>
        </header>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Vegas · дневник тренировок</p>
          <h1 className={styles.heroTitle}>
            Мышцы растут от <em>прогресса</em>
          </h1>
          <p className={styles.heroText}>
            Записывай подходы за секунды — Vegas покажет, где ты растёшь, где застрял и сколько добавить на следующей
            тренировке.
          </p>
          <div className={styles.actions}>
            <ButtonLink to="/register" variant="accent">
              Начать бесплатно
            </ButtonLink>
            <a href="#how" className={styles.ghost}>
              Как это работает ↓
            </a>
          </div>
          <DemoEntry />
          <ul className={styles.trust}>
            <li>Бесплатно</li>
            <li>Без рекламы</li>
            <li>Работает в браузере и с экрана телефона</li>
          </ul>
        </div>
      </section>

      <div className={styles.marquee} aria-hidden="true">
        {/* две одинаковые половины: анимация сдвигает ленту на 50% и незаметно начинает сначала */}
        <div className={styles.marqueeTrack}>
          {[...FEATURES, ...FEATURES].map((feature, i) => (
            <span key={i}>{feature}</span>
          ))}
        </div>
      </div>

      <section id="how" className={styles.section}>
        <p className={styles.eyebrow}>Три шага</p>
        <h2 className={styles.title}>Ты тренируешься. Vegas считает.</h2>
        <div className={styles.steps}>
          <article className={styles.step}>
            <span className={styles.stepNum}>01</span>
            <h3>Записал подход</h3>
            <p>Вес и повторы — двумя кнопками, прямо между подходами. Прошлый результат подставится сам.</p>
            <div data-theme="light" className={styles.mock}>
              <b>Жим лёжа · подход 3</b>
              <div className={styles.stepper}>
                <span>−</span>
                <b>90 кг × 6</b>
                <span>+</span>
              </div>
            </div>
          </article>
          <article className={styles.step}>
            <span className={styles.stepNum}>02</span>
            <h3>Увидел рост</h3>
            <p>График силы по каждому упражнению и пунктир-ориентир: как растут те, кто тренируется регулярно.</p>
            <div data-theme="light" className={styles.mock}>
              <DemoChart />
            </div>
          </article>
          <article className={styles.step}>
            <span className={styles.stepNum}>03</span>
            <h3>Получил подсказку</h3>
            <p>Пора добавить вес, впереди плато или нужна разгрузка — подскажет до того, как ты это почувствуешь.</p>
            <div data-theme="light" className={styles.mock}>
              <div className={styles.todo}>
                <span>Что сделать сейчас</span>
                <b>Добавь 2,5 кг в жиме</b>
                <span>3 тренировки подряд ты сделал 8×4</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section className={`${styles.section} ${styles.split}`}>
        <div className={styles.recordPhoto} role="img" aria-label="Спортсменка в зале, вид со спины">
          <div className={styles.recordTag}>
            <b>Новый рекорд · Тяга в наклоне 70 кг</b>
            <span>+5 кг к прошлому лучшему — в ленте рекордов</span>
          </div>
        </div>
        <div>
          <p className={styles.eyebrow}>Без самообмана</p>
          <h2 className={styles.title}>Плато — это не приговор. Это сигнал.</h2>
          <p className={styles.lead}>
            Большинство бросает зал не из-за лени, а потому что перестают видеть результат. Vegas замечает застой по
            цифрам и предлагает, что поменять.
          </p>
          <ul className={styles.points}>
            {POINTS.map((point) => (
              <li key={point.title}>
                <span className={styles.pointIcon} aria-hidden="true">
                  {point.icon}
                </span>
                <div>
                  <b>{point.title}</b>
                  <span>{point.text}</span>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className={styles.final}>
        <div className={styles.finalContent}>
          <p className={styles.eyebrow}>Первая тренировка — сегодня</p>
          <h2 className={styles.finalTitle}>Через 12 недель ты скажешь себе спасибо</h2>
          <p className={styles.lead}>
            Регистрация — 30 секунд. Добавь Vegas на экран телефона, и он будет открываться как приложение.
          </p>
          <ButtonLink to="/register" variant="accent">
            Создать аккаунт
          </ButtonLink>
        </div>
      </section>

      <footer className={styles.footer}>
        <span>
          Фото: Mehdi Pezhvak, Scott Webb — <a href="https://unsplash.com">Unsplash</a>
        </span>
        <span>
          Уже с нами? <Link to="/login">Войти</Link>
        </span>
      </footer>
    </div>
  );
}

/** График силы демо-атлета: линия, заливка, пунктир-ориентир */
function DemoChart() {
  const gradientId = useId(); // уникальный id: на странице может быть несколько графиков
  const { line, area, reference, last } = chartPaths(DEMO_STRENGTH, DEMO_REFERENCE, 260, 80);
  return (
    <svg className={styles.chart} viewBox="0 0 260 80" role="img" aria-label="График силы растёт неделя за неделей">
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="var(--accent-strong)" stopOpacity="0.35" />
          <stop offset="1" stopColor="var(--accent-strong)" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={reference} className={styles.chartReference} />
      <path d={area} fill={`url(#${gradientId})`} />
      <path d={line} className={styles.chartLine} />
      <circle cx={last.x} cy={last.y} r="4.5" className={styles.chartDot} />
    </svg>
  );
}

/** Полоса браузера/статус-бар телефона — тёмная, пока открыта страница (потом вернём цвет темы) */
function useDarkBrowserBar() {
  useEffect(() => {
    const meta = document.querySelector('meta[name="theme-color"]');
    const previous = meta?.getAttribute('content');
    meta?.setAttribute('content', THEME_COLOR.dark);
    return () => {
      if (previous) meta?.setAttribute('content', previous);
    };
  }, []);
}
