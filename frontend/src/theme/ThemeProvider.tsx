import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { SKIN_FONTS, loadSkin, saveSkin, skinInfo, type Skin } from './skin';
import { THEME_COLOR, loadThemeChoice, resolveTheme, saveThemeChoice, type Theme, type ThemeChoice } from './theme';

interface ThemeContextValue {
  choice: ThemeChoice;
  theme: Theme;
  setChoice: (choice: ThemeChoice) => void;
  /** стиль оформления (docs/styles.md) */
  skin: Skin;
  setSkin: (skin: Skin) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const darkQuery = () => window.matchMedia('(prefers-color-scheme: dark)');

/**
 * Хранит выбор темы и стиля и ставит <html data-theme="light|dark" data-skin="classic|notebook">.
 * Все цвета в tokens.css зависят от data-theme, детали пробных стилей (styles/skins/*.css) — от data-skin,
 * поэтому смена перекрашивает всё сразу, без перезагрузки.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [choice, setChoiceState] = useState<ThemeChoice>(loadThemeChoice);
  const [skin, setSkinState] = useState<Skin>(loadSkin);
  const [systemDark, setSystemDark] = useState(() => darkQuery().matches);

  // "Как в системе": следим, если пользователь переключит тему ОС, пока сайт открыт
  useEffect(() => {
    const query = darkQuery();
    const onChange = (event: MediaQueryListEvent) => setSystemDark(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  // у стиля только светлый вариант — тёмную тему не включаем, выбор пользователя при этом не теряем
  const theme = skinInfo(skin).forcesLight ? 'light' : resolveTheme(choice, systemDark);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.skin = skin;
    // тема могла быть выбрана вручную, не как в системе — полосу браузера красим под неё же
    const color = theme === 'light' ? skinInfo(skin).themeColor : THEME_COLOR.dark;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', color);
  }, [theme, skin]);

  // шрифты стиля подключаем, только когда он выбран (один <link> на стиль, повторно не добавляем)
  useEffect(() => {
    const href = SKIN_FONTS[skin];
    if (!href || document.querySelector(`link[data-skin-fonts="${skin}"]`)) {
      return;
    }
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = href;
    link.dataset.skinFonts = skin;
    document.head.appendChild(link);
  }, [skin]);

  const setChoice = (next: ThemeChoice) => {
    setChoiceState(next);
    saveThemeChoice(next);
  };

  const setSkin = (next: Skin) => {
    setSkinState(next);
    saveSkin(next);
  };

  return <ThemeContext.Provider value={{ choice, theme, setChoice, skin, setSkin }}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const value = useContext(ThemeContext);
  if (!value) {
    throw new Error('useTheme() вызван вне <ThemeProvider>');
  }
  return value;
}
