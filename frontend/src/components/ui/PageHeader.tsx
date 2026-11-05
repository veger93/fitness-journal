import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import styles from './PageHeader.module.css';

interface PageHeaderProps {
  title: ReactNode;
  /** над заголовком: «Привет, Алексей», «Отчёт за месяц» */
  eyebrow?: ReactNode;
  /** под заголовком: дата, «Спина · штанга», ссылка «Отчёт за месяц →» */
  subtitle?: ReactNode;
  /** «← Все упражнения»: ссылка назад над заголовком */
  back?: { to: string; label: string };
  /** одно действие справа (на телефоне — под заголовком) */
  action?: ReactNode;
}

/**
 * Шапка страницы — одна на всех экранах (дизайн-система: PageHeader).
 * Заголовок всегда --text-h1 (40px на ПК, 28px на телефоне): своих размеров заголовков на страницах нет.
 */
export function PageHeader({ title, eyebrow, subtitle, back, action }: PageHeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.text}>
        {back && (
          <Link to={back.to} className={styles.back}>
            ← {back.label}
          </Link>
        )}
        {eyebrow && <p className={styles.eyebrow}>{eyebrow}</p>}
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <div className={styles.subtitle}>{subtitle}</div>}
      </div>
      {action && <div className={styles.action}>{action}</div>}
    </header>
  );
}
