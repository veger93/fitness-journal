import { useEffect, useId, useRef, useState, type ReactNode, type RefObject } from 'react';
import styles from './Dialog.module.css';

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  /** есть — слева в шапке кнопка "←" (шаг назад внутри окна: из карточки к списку, из категории к плиткам) */
  onBack?: () => void;
  /** своё содержимое левого угла шапки вместо "←" (миниатюра упражнения в окне подхода) */
  leading?: ReactNode;
  children: ReactNode;
  /** окно "вырастает" из этого элемента и сворачивается обратно в него (кнопка "Я ▾" в шапке) */
  originRef?: RefObject<HTMLElement | null>;
}

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/** Точка, из которой растёт окно, — в координатах самого окна (для transform-origin) */
function originPoint(dialog: HTMLElement, origin: HTMLElement | null | undefined): string {
  if (!origin) {
    return 'center';
  }
  const d = dialog.getBoundingClientRect();
  const o = origin.getBoundingClientRect();
  return `${o.left + o.width / 2 - d.left}px ${o.top + o.height / 2 - d.top}px`;
}

/**
 * Анимации открытия/закрытия — Web Animations API (element.animate): JS решает, когда закрывать <dialog>
 * (только после того, как анимация доиграла), а CSS так не умеет во всех браузерах.
 * Из кнопки — "капля": растёт из точки с лёгким перелётом (как капсула в нижней панели); без кнопки — всплывает снизу.
 */
function grow(dialog: HTMLElement, origin: HTMLElement | null | undefined, reverse = false): Animation {
  // прошлая анимация (например, окно закрыли и тут же открыли) больше не нужна
  dialog.getAnimations({ subtree: false }).forEach((animation) => animation.cancel());
  dialog.style.transformOrigin = originPoint(dialog, origin);
  const frames: Keyframe[] = origin
    ? [
        { opacity: 0, transform: 'scale(0.15)' },
        { opacity: 1, transform: 'scale(1)' },
      ]
    : [
        { opacity: 0, transform: 'translateY(24px) scale(0.97)' },
        { opacity: 1, transform: 'none' },
      ];
  const options: KeyframeAnimationOptions = {
    duration: reverse ? 200 : origin ? 380 : 240,
    // открытие с перелётом (1.25 > 1 — окно чуть "проскакивает" и возвращается), закрытие — просто быстро
    easing: reverse ? 'cubic-bezier(0.4, 0, 1, 1)' : origin ? 'cubic-bezier(0.2, 0.9, 0.3, 1.25)' : 'ease-out',
    direction: reverse ? 'reverse' : 'normal',
    fill: 'both',
  };
  if (origin) {
    // из кнопки-"таблетки" окно растёт круглым и становится карточкой. Отдельной анимацией и без перелёта:
    // с перелётом радиус ушёл бы меньше нуля, и углы на миг стали бы острыми. Радиус — числом из CSS
    const radius = getComputedStyle(dialog).borderRadius;
    dialog.animate({ borderRadius: ['999px', radius] }, { ...options, easing: 'ease-out' });
  }
  try {
    // фон за окном — плавно затемняем; ::backdrop в animate() умеют не все браузеры — тогда без него
    dialog.animate({ opacity: [0, 1] }, { ...options, easing: 'linear', pseudoElement: '::backdrop' });
  } catch {
    // ничего страшного: фон появится сразу
  }
  return dialog.animate(frames, options);
}

/**
 * Модальное окно на встроенном <dialog>. showModal() даёт бесплатно то, что руками писать долго:
 * затемнение фона, фокус внутри окна, закрытие по Esc, недоступность страницы под окном.
 * Esc на телефоне нет — поэтому в шапке всегда крестик.
 * Шапка — три колонки одинаковой ширины по краям: так заголовок остаётся по центру, что бы ни стояло слева.
 */
export function Dialog({ open, onClose, title, subtitle, onBack, leading, children, originRef }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  // содержимое остаётся на месте, пока доигрывает анимация закрытия (open уже false)
  const [mounted, setMounted] = useState(open);
  if (open && !mounted) {
    setMounted(true); // поправить состояние прямо во время рендера — разрешённый в React приём
  }

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) {
      return;
    }
    const animate = !prefersReducedMotion() && typeof dialog.animate === 'function';
    if (open && !dialog.open) {
      dialog.showModal();
      if (animate) grow(dialog, originRef?.current);
    } else if (!open && dialog.open) {
      if (!animate) {
        dialog.close();
        setMounted(false);
        return;
      }
      grow(dialog, originRef?.current, true)
        .finished.then(() => {
          dialog.close();
          setMounted(false);
        })
        .catch(() => {
          // анимацию прервали (окно снова открыли) — ничего не делаем
        });
    }
  }, [open, originRef]);

  return (
    // close — после dialog.close() (в том числе нашего, по окончании анимации): синхронизируем состояние React
    <dialog
      ref={ref}
      className={styles.dialog}
      onClose={onClose}
      // Esc: не даём браузеру закрыть окно мгновенно — закрываем сами, с анимацией
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      aria-labelledby={titleId}
    >
      {mounted && (
        <div className={styles.body}>
          <div className={styles.header}>
            <div className={styles.side}>
              {leading ??
                (onBack && (
                  <button type="button" className={styles.iconButton} aria-label="Назад" onClick={onBack}>
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M15 6l-6 6 6 6" />
                    </svg>
                  </button>
                ))}
            </div>
            <div className={styles.heading}>
              {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
              <h2 id={titleId} className={styles.title}>
                {title}
              </h2>
            </div>
            <div className={`${styles.side} ${styles.sideEnd}`}>
              <button type="button" className={styles.iconButton} aria-label="Закрыть" onClick={onClose}>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            </div>
          </div>
          {children}
        </div>
      )}
    </dialog>
  );
}
