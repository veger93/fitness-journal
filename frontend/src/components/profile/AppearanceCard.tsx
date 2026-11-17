import { SKINS, skinInfo } from '../../theme/skin';
import type { ThemeChoice } from '../../theme/theme';
import { useTheme } from '../../theme/ThemeProvider';
import { Card } from '../ui/Card';
import { ChoiceCards } from '../ui/ChoiceCards';
import { SegmentedControl } from '../ui/SegmentedControl';
import styles from './Profile.module.css';

const THEME_OPTIONS: { value: ThemeChoice; label: string }[] = [
  { value: 'light', label: 'Светлая' },
  { value: 'dark', label: 'Тёмная' },
  { value: 'system', label: 'Как в системе' },
];

const SKIN_OPTIONS = SKINS.map((s) => ({ value: s.id, title: s.title, description: s.description }));

/**
 * Профиль → «Оформление»: стиль (docs/styles.md) и тема.
 * Всё хранится в этом браузере (localStorage), на сервер не уходит.
 */
export function AppearanceCard() {
  const { choice, setChoice, skin, setSkin } = useTheme();
  const fixed = skinInfo(skin).fixedTheme;

  return (
    <Card as="section">
      <h2 className={styles.sectionTitle}>Оформление</h2>
      <ChoiceCards legend="Стиль" options={SKIN_OPTIONS} value={skin} onChange={setSkin} />
      {fixed ? (
        <p className={styles.muted}>
          У стиля «{skinInfo(skin).title}» пока есть только {fixed === 'light' ? 'светлая' : 'тёмная'} тема.
        </p>
      ) : (
        <>
          <SegmentedControl legend="Тема оформления" options={THEME_OPTIONS} value={choice} onChange={setChoice} />
          <p className={styles.muted}>
            «Как в системе» — тема переключится вместе с настройкой телефона или компьютера.
          </p>
        </>
      )}
    </Card>
  );
}
