import { useAuth } from '../auth/AuthProvider';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { useTheme } from '../theme/ThemeProvider';
import type { ThemeChoice } from '../theme/theme';
import styles from './ProfilePage.module.css';

const THEME_OPTIONS: { value: ThemeChoice; label: string }[] = [
  { value: 'light', label: 'Светлая' },
  { value: 'dark', label: 'Тёмная' },
  { value: 'system', label: 'Как в системе' },
];

export function ProfilePage() {
  const { choice, setChoice } = useTheme();
  const { logout } = useAuth();

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Профиль</h1>
      <Card>
        <h2 className={styles.sectionTitle}>Оформление</h2>
        <SegmentedControl legend="Тема оформления" options={THEME_OPTIONS} value={choice} onChange={setChoice} />
        <p className={styles.muted}>«Как в системе» — тема переключится вместе с настройкой телефона или компьютера.</p>
      </Card>
      <Button variant="secondary" className={styles.logout} onClick={logout}>
        Выйти
      </Button>
    </div>
  );
}
