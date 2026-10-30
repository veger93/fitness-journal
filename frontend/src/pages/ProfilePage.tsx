import { useQuery } from '@tanstack/react-query';
import { authApi } from '../api/auth';
import { usersApi } from '../api/users';
import { useAuth } from '../auth/AuthProvider';
import { BodyWeightCard } from '../components/profile/BodyWeightCard';
import { ProfileForm } from '../components/profile/ProfileForm';
import { RemindersCard } from '../components/profile/RemindersCard';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { DeleteAccountCard } from '../components/account/DeleteAccountCard';
import { EmailVerifyCard } from '../components/account/EmailVerifyCard';
import { ClientProfileCard } from '../components/trainer/ClientProfileCard';
import { MyTrainerCard } from '../components/trainer/MyTrainerCard';
import { TrainerModeCard } from '../components/trainer/TrainerModeCard';
import { useTheme } from '../theme/ThemeProvider';
import { useActingClient } from '../trainer/ActingClientProvider';
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
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me });
  const profile = useQuery({ queryKey: ['profile'], queryFn: usersApi.profile });
  const { client: acting } = useActingClient();

  // тренер в дневнике клиента: "Профиль" — карточка клиента; свой профиль — после "✕ К себе"
  if (acting) {
    return (
      <div className={styles.page}>
        <h1 className={styles.title}>{acting.displayName}</h1>
        <ClientProfileCard clientId={acting.id} />
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div>
        <h1 className={styles.title}>Профиль</h1>
        {me.data?.email && (
          <p className={styles.muted}>
            {me.data.email}
            {me.data.emailVerified && ' · подтверждён ✓'}
          </p>
        )}
      </div>

      <EmailVerifyCard />

      <Card>
        <h2 className={styles.sectionTitle}>Физические данные</h2>
        {profile.data ? (
          // форма появляется только когда профиль загружен: useState в ней берёт начальные значения один раз
          <ProfileForm profile={profile.data} submitLabel="Сохранить" withWeight={false} />
        ) : (
          <p className={styles.muted}>{profile.isError ? profile.error.message : 'Загружаем…'}</p>
        )}
      </Card>

      {profile.data && <BodyWeightCard currentWeightKg={profile.data.currentWeightKg} />}

      <RemindersCard />

      <MyTrainerCard />

      <TrainerModeCard />

      <Card>
        <h2 className={styles.sectionTitle}>Оформление</h2>
        <SegmentedControl legend="Тема оформления" options={THEME_OPTIONS} value={choice} onChange={setChoice} />
        <p className={styles.muted}>«Как в системе» — тема переключится вместе с настройкой телефона или компьютера.</p>
      </Card>

      <Button variant="secondary" className={styles.logout} onClick={logout}>
        Выйти
      </Button>

      <DeleteAccountCard />
    </div>
  );
}
