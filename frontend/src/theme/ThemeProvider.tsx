import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { THEME_COLOR, loadThemeChoice, resolveTheme, saveThemeChoice, type Theme, type ThemeChoice } from './theme';

interface ThemeContextValue {
  choice: ThemeChoice;
  theme: Theme;
  setChoice: (choice: ThemeChoice) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const darkQuery = () => window.matchMedia('(prefers-color-scheme: dark)');

/**
 * Хранит выбор темы и ставит <html data-theme="light|dark">.
 * Все цвета в tokens.css зависят от этого атрибута — поэтому смена темы перекрашивает всё сразу.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [choice, setChoiceState] = useState<ThemeChoice>(loadThemeChoice);
  const [systemDark, setSystemDark] = useState(() => darkQuery().matches);

  // "Как в системе": следим, если пользователь переключит тему ОС, пока сайт открыт
  useEffect(() => {
    const query = darkQuery();
    const onChange = (event: MediaQueryListEvent) => setSystemDark(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  const theme = resolveTheme(choice, systemDark);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    // тема могла быть выбрана вручную, не как в системе — полосу браузера красим под неё же
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLOR[theme]);
  }, [theme]);

  const setChoice = (next: ThemeChoice) => {
    setChoiceState(next);
    saveThemeChoice(next);
  };

  return <ThemeContext.Provider value={{ choice, theme, setChoice }}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const value = useContext(ThemeContext);
  if (!value) {
    throw new Error('useTheme() вызван вне <ThemeProvider>');
  }
  return value;
}
