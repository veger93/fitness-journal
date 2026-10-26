import { Link } from 'react-router-dom';
import styles from './BackLink.module.css';

/** Ссылка "назад" над заголовком страницы: "← Все упражнения". */
export function BackLink({ to, children }: { to: string; children: string }) {
  return (
    <Link to={to} className={styles.back}>
      ← {children}
    </Link>
  );
}
