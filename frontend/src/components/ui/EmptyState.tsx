import { Link } from 'react-router-dom';
import { Card } from './Card';
import styles from './EmptyState.module.css';

/**
 * Пустой экран: что здесь появится и что сделать, чтобы появилось. Один вид на всех страницах —
 * тёмная (в тёмной теме светлая) карточка, как на главной до первой тренировки.
 */
export function EmptyState({
  title,
  text,
  action,
}: {
  title: string;
  text: string;
  action?: { to: string; label: string };
}) {
  return (
    <Card variant="inverse" size="section">
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.text}>{text}</p>
      {action && (
        <Link to={action.to} className={styles.action}>
          {action.label} →
        </Link>
      )}
    </Card>
  );
}
