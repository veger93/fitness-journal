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
  /**
   * Окно-меню: открывается прямо под этим элементом (кнопка "Я ▾" в шапке) и раскрывается вниз, как шторка.
   * Без него — обычное окно по центру, мягко всплывает снизу.
   */
  anchorRef?: RefObject<HTMLElement | null>;
}

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/** Отступ окна-меню от кнопки и от краёв экрана, px */
const ANCHOR_GAP = 8;
const SCREEN_GAP = 16;
const MENU_WIDTH = 360;

/** Ставит окно-меню под кнопку, правым краем по её правому краю (но целиком на экране, не ближе 16px к краям) */
function placeUnder(dialog: HTMLElement, anchor: HTMLElement) {
  const rect = anchor.getBoundingClientRect();
  dialog.style.top = `${rect.bottom + ANCHOR_GAP}px`;
  // ширина окна — как в CSS (.menu): не шире 360px и экрана; если под кнопкой не помещается — сдвигаем влево
  const width = Math.min(MENU_WIDTH, window.innerWidth - 2 * SCREEN_GAP);
  const right = Math.min(Math.max(SCREEN_GAP, window.innerWidth - rect.right), window.innerWidth - SCREEN_GAP - width);
  dialog.style.right = `${right}px`;
  // не ниже экрана: длинный список прокручивается внутри окна
  dialog.style.maxHeight = `${window.innerHeight - rect.bottom - ANCHOR_GAP - SCREEN_GAP}px`;
}

const EASE_OUT = 'cubic-bezier(0.16, 1, 0.3, 1)';
const EASE_IN = 'cubic-bezier(0.4, 0, 1, 1)';

/**
 * Анимации открытия/закрытия — Web Animations API (element.animate): JS решает, когда закрывать <dialog>
 * (только после того, как анимация доиграла), а CSS так не умеет во всех браузерах.
 * - окно-меню: раскрывается вниз из-под кнопки (clip-path "отрезает" низ и плавно его открывает),
 *   строки с атрибутом data-stagger появляются по очереди;
 * - обычное окно: поднимается на 24px и проявляется.
 * Закрытие — то же самое в обратную сторону, быстрее.
 */
function animate(dialog: HTMLElement, menu: boolean, reverse = false): Animation {
  // прошлая анимация (окно закрыли и тут же открыли) больше не нужна
  dialog.getAnimations({ subtree: true }).forEach((animation) => animation.cancel());
  const options: KeyframeAnimationOptions = {
    duration: reverse ? 180 : menu ? 340 : 260,
    easing: reverse ? EASE_IN : EASE_OUT,
    direction: reverse ? 'reverse' : 'normal',
    fill: 'both',
  };
  try {
    // фон за окном — плавно затемняем; ::backdrop в animate() умеют не все браузеры — тогда без него
    dialog.animate({ opacity: [0, 1] }, { ...options, easing: 'linear', pseudoElement: '::backdrop' });
  } catch {
    // ничего страшного: фон появится сразу
  }
  if (!menu) {
    return dialog.animate(
      [
        { opacity: 0, transform: 'translateY(24px) scale(0.97)' },
        { opacity: 1, transform: 'none' },
      ],
      options,
    );
  }
  if (!reverse) {
    dialog.querySelectorAll('[data-stagger] > *').forEach((row, i) =>
      row.animate(
        [
          { opacity: 0, transform: 'translateY(-6px)' },
          { opacity: 1, transform: 'none' },
        ],
        { duration: 220, delay: 90 + i * 45, easing: 'ease-out', fill: 'both' },
      ),
    );
  }
  const radius = getComputedStyle(dialog).borderRadius;
  return dialog.animate(
    [
      { clipPath: `inset(0 0 100% 0 round ${radius})`, opacity: 0.6, transform: 'translateY(-6px)' },
      { clipPath: `inset(0 0 0% 0 round ${radius})`, opacity: 1, transform: 'none' },
    ],
    options,
  );
}

/**
 * Модальное окно на встроенном <dialog>. showModal() даёт бесплатно то, что руками писать долго:
 * затемнение фона, фокус внутри окна, закрытие по Esc, недоступность страницы под окном.
 * Esc на телефоне нет — поэтому в шапке всегда крестик.
 * Шапка — три колонки одинаковой ширины по краям: так заголовок остаётся по центру, что бы ни стояло слева.
 */
export function Dialog({ open, onClose, title, subtitle, onBack, leading, children, anchorRef }: DialogProps) {
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
    const canAnimate = !prefersReducedMotion() && typeof dialog.animate === 'function';
    const anchor = anchorRef?.current ?? null;
    if (open && !dialog.open) {
      if (anchor) placeUnder(dialog, anchor);
      dialog.showModal();
      if (canAnimate) animate(dialog, anchor !== null);
    } else if (!open && dialog.open) {
      if (!canAnimate) {
        dialog.close();
        setMounted(false);
        return;
      }
      animate(dialog, anchor !== null, true)
        .finished.then(() => {
          dialog.close();
          setMounted(false);
        })
        .catch(() => {
          // анимацию прервали (окно снова открыли) — ничего не делаем
        });
    }
  }, [open, anchorRef]);

  return (
    // close — после dialog.close() (в том числе нашего, по окончании анимации): синхронизируем состояние React
    <dialog
      ref={ref}
      className={anchorRef ? `${styles.dialog} ${styles.menu}` : styles.dialog}
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
