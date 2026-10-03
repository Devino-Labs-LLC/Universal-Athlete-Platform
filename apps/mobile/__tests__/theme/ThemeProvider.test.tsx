import AsyncStorage from '@react-native-async-storage/async-storage';
import { act, fireEvent, render, waitFor } from '@testing-library/react-native';
import { Appearance, Text } from 'react-native';

import {
  THEME_PREFERENCE_KEY,
  ThemeProvider,
  useAppTheme,
  useThemePreference,
} from '@/src/app/theme/ThemeProvider';
import { brand } from '@/src/app/theme/tokens';

const mockStorage = new Map<string, string>();

jest.mock('@react-native-async-storage/async-storage', () => ({
  __esModule: true,
  default: {
    getItem: jest.fn(async (key: string) => mockStorage.get(key) ?? null),
    setItem: jest.fn(async (key: string, value: string) => {
      mockStorage.set(key, value);
    }),
    removeItem: jest.fn(async (key: string) => {
      mockStorage.delete(key);
    }),
    clear: jest.fn(async () => {
      mockStorage.clear();
    }),
  },
}));

jest.mock('expo-status-bar', () => ({
  StatusBar: () => null,
}));

function ThemeProbe() {
  const theme = useAppTheme();
  const { preference, resolvedMode, setPreference } = useThemePreference();
  return (
    <>
      <Text testID="mode">{theme.mode}</Text>
      <Text testID="resolved">{resolvedMode}</Text>
      <Text testID="preference">{preference}</Text>
      <Text testID="primary">{theme.colors.brandPrimary}</Text>
      <Text testID="set-dark" onPress={() => setPreference('dark')}>
        set-dark
      </Text>
      <Text testID="set-light" onPress={() => setPreference('light')}>
        set-light
      </Text>
      <Text testID="set-system" onPress={() => setPreference('system')}>
        set-system
      </Text>
    </>
  );
}

describe('ThemeProvider preference + system resolution', () => {
  beforeEach(() => {
    mockStorage.clear();
    jest.spyOn(Appearance, 'getColorScheme').mockReturnValue('light');
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('defaults to system preference and follows OS light scheme', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(getByTestId('preference').props.children).toBe('system');
      expect(getByTestId('resolved').props.children).toBe('light');
      expect(getByTestId('primary').props.children).toBe(brand.lime600);
    });
  });

  it('manual dark override ignores system light and persists', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(getByTestId('preference').props.children).toBe('system');
    });

    await act(async () => {
      fireEvent.press(getByTestId('set-dark'));
    });

    await waitFor(() => {
      expect(getByTestId('preference').props.children).toBe('dark');
      expect(getByTestId('resolved').props.children).toBe('dark');
      expect(getByTestId('mode').props.children).toBe('dark');
      expect(getByTestId('primary').props.children).toBe(brand.lime500);
    });

    await expect(AsyncStorage.getItem(THEME_PREFERENCE_KEY)).resolves.toBe('dark');
  });

  it('hydrates stored preference on mount', async () => {
    await AsyncStorage.setItem(THEME_PREFERENCE_KEY, 'dark');

    const { getByTestId } = await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(getByTestId('preference').props.children).toBe('dark');
      expect(getByTestId('resolved').props.children).toBe('dark');
    });
  });

  it('manual light override forces light tokens', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    await act(async () => {
      fireEvent.press(getByTestId('set-light'));
    });

    await waitFor(() => {
      expect(getByTestId('preference').props.children).toBe('light');
      expect(getByTestId('resolved').props.children).toBe('light');
      expect(getByTestId('primary').props.children).toBe(brand.lime600);
    });
  });

  it('restores system-following when preference returns to system', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    await waitFor(() => expect(getByTestId('preference').props.children).toBe('system'));

    await act(async () => {
      fireEvent.press(getByTestId('set-dark'));
    });
    await waitFor(() => expect(getByTestId('resolved').props.children).toBe('dark'));

    await act(async () => {
      fireEvent.press(getByTestId('set-system'));
    });
    await waitFor(() => {
      expect(getByTestId('preference').props.children).toBe('system');
      expect(getByTestId('resolved').props.children).toBe('light');
    });
  });
});
