import AsyncStorage from '@react-native-async-storage/async-storage';
import { StatusBar } from 'expo-status-bar';
import {
  createContext,
  PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useColorScheme } from 'react-native';

import {
  AppTheme,
  createTheme,
  resolveThemeMode,
  ThemeMode,
  ThemePreference,
} from '@/src/app/theme/tokens';

const THEME_PREFERENCE_KEY = 'uap.themePreference';

function isThemePreference(value: string | null): value is ThemePreference {
  return value === 'light' || value === 'dark' || value === 'system';
}

interface ThemeContextValue {
  theme: AppTheme;
  /** Resolved light/dark after applying preference + system. */
  resolvedMode: ThemeMode;
  preference: ThemePreference;
  setPreference: (next: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: PropsWithChildren) {
  const systemScheme = useColorScheme();
  const [preference, setPreferenceState] = useState<ThemePreference>('system');
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const stored = await AsyncStorage.getItem(THEME_PREFERENCE_KEY);
        if (!cancelled && isThemePreference(stored)) {
          setPreferenceState(stored);
        }
      } catch {
        // Keep default system preference when storage is unavailable.
      } finally {
        if (!cancelled) {
          setHydrated(true);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const setPreference = useCallback((next: ThemePreference) => {
    setPreferenceState(next);
    void AsyncStorage.setItem(THEME_PREFERENCE_KEY, next).catch(() => {
      // Persistence failure must not break runtime theme.
    });
  }, []);

  const resolvedMode = resolveThemeMode(preference, systemScheme);
  const theme = useMemo(() => createTheme(resolvedMode), [resolvedMode]);

  const value = useMemo(
    () => ({
      theme,
      resolvedMode,
      preference,
      setPreference,
    }),
    [theme, resolvedMode, preference, setPreference],
  );

  return (
    <ThemeContext.Provider value={value}>
      {/* Avoid flashing wrong status bar until preference hydrate completes. */}
      {hydrated ? (
        <StatusBar style={resolvedMode === 'dark' ? 'light' : 'dark'} />
      ) : (
        <StatusBar style={systemScheme === 'dark' ? 'light' : 'dark'} />
      )}
      {children}
    </ThemeContext.Provider>
  );
}

export function useAppTheme(): AppTheme {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useAppTheme must be used within ThemeProvider');
  }
  return ctx.theme;
}

/** Preference API for optional user theme toggle (system / light / dark). */
export function useThemePreference(): {
  preference: ThemePreference;
  resolvedMode: ThemeMode;
  setPreference: (next: ThemePreference) => void;
} {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useThemePreference must be used within ThemeProvider');
  }
  return {
    preference: ctx.preference,
    resolvedMode: ctx.resolvedMode,
    setPreference: ctx.setPreference,
  };
}

export { THEME_PREFERENCE_KEY };
