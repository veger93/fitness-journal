import { useState } from 'react';
import type { Profile } from '../../api/users';
import { plural } from '../../lib/format';
import { Card } from '../ui/Card';
import { GENDERS, LEVELS, ProfileForm } from './ProfileForm';
import styles from './Profile.module.css';

/**
 * Физические данные в профиле: обычно — только чтение (дизайн-система: ProfileSummary), по «Изменить» — форма.
 * Пол, год рождения и рост меняются редко: открытая форма с выбором и кнопкой «Сохранить» на каждом заходе
 * только мешала и провоцировала случайно что-то поменять.
 */
export function ProfileSummary({ profile }: { profile: Profile }) {
  const [editing, setEditing] = useState(false);

  return (
    <Card as="section">
      <div className={styles.cardHead}>
        <h2 className={styles.sectionTitle}>Физические данные</h2>
        {!editing && (
          <button type="button" className={styles.linkButton} onClick={() => setEditing(true)}>
            Изменить
          </button>
        )}
      </div>
      {editing ? (
        // форма берёт начальные значения один раз (useState) — монтируем её заново на каждое «Изменить»
        <ProfileForm
          profile={profile}
          submitLabel="Сохранить"
          withWeight={false}
          onSaved={() => setEditing(false)}
          onCancel={() => setEditing(false)}
        />
      ) : (
        <dl className={styles.facts}>
          <Fact label="Пол" value={GENDERS.find((g) => g.value === profile.gender)?.title} />
          <Fact label="Уровень подготовки" value={LEVELS.find((l) => l.value === profile.experienceLevel)?.title} />
          <Fact label="Год рождения" value={profile.birthYear ? ageText(profile.birthYear) : null} />
          <Fact label="Рост" value={profile.heightCm ? `${profile.heightCm} см` : null} />
        </dl>
      )}
    </Card>
  );
}

function Fact({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value ?? 'не указан'}</dd>
    </div>
  );
}

/** 1994 -> "1994 · 32 года" (возраст — примерно: день рождения не знаем) */
function ageText(birthYear: number): string {
  const age = new Date().getFullYear() - birthYear;
  return `${birthYear} · ${plural(age, 'год', 'года', 'лет')}`;
}
