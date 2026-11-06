import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { usersApi } from '../api/users';
import { useAuth } from '../auth/AuthProvider';
import { BodyWeightCard } from '../components/profile/BodyWeightCard';
import { ProfileSummary } from '../components/profile/ProfileSummary';
import { RemindersCard } from '../components/profile/RemindersCard';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { PageColumns } from '../components/ui/PageColumns';
import { PageHeader } from '../components/ui/PageHeader';
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
  const demo = me.data?.demo ?? false;
  const navigate = useNavigate();
  // из демо — на лендинг, а не на форму входа (входить гостю нечем). Сначала переход, потом выход:
  // наоборот закрытая страница успела бы сама увести на /login
  const exitDemo = () => {
    navigate('/', { replace: true });
    void logout();
  };

  // тренер в дневнике клиента: "Профиль" — карточка клиента; свой профиль — после "✕ К себе"
  if (acting) {
    return (
      <div className={styles.page}>
        <PageHeader eyebrow="Профиль клиента" title={acting.displayName} />
        <ClientProfileCard clientId={acting.id} />
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Профиль"
        subtitle={me.data?.email && `${me.data.email}${me.data.emailVerified ? ' · подтверждён ✓' : ''}`}
      />

      {/* ПК: «я» — данные и вес (2/3) | настройки — напоминания, тренер, тема, выход (1/3) */}
      <PageColumns
        aside={
          <>
            {/* напоминания — письма; у демо нет почты (docs/demo.md) */}
            {!demo && <RemindersCard />}

            <MyTrainerCard />

            <TrainerModeCard />

            <Card as="section">
              <h2 className={styles.sectionTitle}>Оформление</h2>
              <SegmentedControl legend="Тема оформления" options={THEME_OPTIONS} value={choice} onChange={setChoice} />
              <p className={styles.muted}>
                «Как в системе» — тема переключится вместе с настройкой телефона или компьютера.
              </p>
            </Card>

            {/* внизу одной строкой: "Выйти" слева, редкое "Удалить аккаунт" справа, по нижнему краю */}
            <div className={styles.bottomRow}>
              <Button variant="secondary" onClick={demo ? exitDemo : logout}>
                {demo ? 'Выйти из демо' : 'Выйти'}
              </Button>
              {/* демо удаляется само через сутки — удалять его вручную (с паролем, которого нет) не нужно */}
              {!demo && <DeleteAccountCard />}
            </div>
          </>
        }
      >
        <EmailVerifyCard />

        {profile.data ? (
          <ProfileSummary profile={profile.data} />
        ) : (
          <p className={styles.muted}>{profile.isError ? profile.error.message : 'Загружаем…'}</p>
        )}

        {profile.data && <BodyWeightCard currentWeightKg={profile.data.currentWeightKg} />}
      </PageColumns>
    </div>
  );
}
