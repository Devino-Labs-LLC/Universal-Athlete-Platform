export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const;

export const radius = {
  sm: 6,
  md: 10,
  lg: 16,
  full: 999,
} as const;

export const typography = {
  title: 24,
  subtitle: 18,
  body: 16,
  caption: 13,
} as const;

export interface ColorTokens {
  background: string;
  surface: string;
  text: string;
  textMuted: string;
  border: string;
  primary: string;
  primaryText: string;
  danger: string;
  success: string;
  warning: string;
  info: string;
}

export const lightColors: ColorTokens = {
  background: '#F8FAFC',
  surface: '#FFFFFF',
  text: '#0F172A',
  textMuted: '#64748B',
  border: '#CBD5E1',
  primary: '#0D9488',
  primaryText: '#FFFFFF',
  danger: '#DC2626',
  success: '#059669',
  warning: '#D97706',
  info: '#0284C7',
};

export const darkColors: ColorTokens = {
  background: '#0F172A',
  surface: '#1E293B',
  text: '#F8FAFC',
  textMuted: '#94A3B8',
  border: '#334155',
  primary: '#2DD4BF',
  primaryText: '#042F2E',
  danger: '#F87171',
  success: '#34D399',
  warning: '#FBBF24',
  info: '#38BDF8',
};

export type ThemeMode = 'light' | 'dark';

export interface AppTheme {
  mode: ThemeMode;
  colors: ColorTokens;
  spacing: typeof spacing;
  radius: typeof radius;
  typography: typeof typography;
}

export function createTheme(mode: ThemeMode): AppTheme {
  return {
    mode,
    colors: mode === 'dark' ? darkColors : lightColors,
    spacing,
    radius,
    typography,
  };
}
