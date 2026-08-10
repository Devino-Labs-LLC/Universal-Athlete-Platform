import { createContext, PropsWithChildren, useContext, useMemo } from 'react';
import { useColorScheme } from 'react-native';

import { AppTheme, createTheme } from '@/src/app/theme/tokens';

const ThemeContext = createContext<AppTheme>(createTheme('light'));

export function ThemeProvider({ children }: PropsWithChildren) {
  const scheme = useColorScheme();
  const theme = useMemo(
    () => createTheme(scheme === 'dark' ? 'dark' : 'light'),
    [scheme],
  );

  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}

export function useAppTheme(): AppTheme {
  return useContext(ThemeContext);
}
