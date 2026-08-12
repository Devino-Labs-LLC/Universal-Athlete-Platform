/**
 * UAP mobile semantic theme — aligned with frozen Web V1 brand language,
 * expressed for React Native (no CSS variables).
 */

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const radius = {
  sm: 6,
  md: 10,
  lg: 12,
  xl: 16,
  full: 999,
} as const;

/** Native type scale (points). System UI font; SpaceMono is not used for body. */
export const typography = {
  display: 32,
  pageTitle: 24,
  sectionTitle: 17,
  body: 16,
  bodyMuted: 15,
  button: 16,
  caption: 13,
  eyebrow: 11,
  metric: 28,
} as const;

/** Brand accents shared across themes (Web V1). */
export const brand = {
  lime400: '#bef264',
  lime500: '#a3e635',
  lime600: '#84cc16',
  cyan400: '#22d3ee',
  cyan500: '#06b6d4',
  cyan600: '#0891b2',
  ai400: '#c084fc',
  ai500: '#a855f7',
  ai600: '#9333ea',
} as const;

export interface ColorTokens {
  background: string;
  surface: string;
  surfaceElevated: string;
  surfaceMuted: string;
  border: string;
  borderStrong: string;
  text: string;
  textMuted: string;
  /** Lime — meaningful primary actions */
  primary: string;
  primaryPressed: string;
  primaryText: string;
  primaryMuted: string;
  /** Cyan — navigation / selection / information */
  accentCyan: string;
  accentCyanMuted: string;
  /** Reserved for future AI concepts only */
  accentAi: string;
  accentAiMuted: string;
  danger: string;
  dangerMuted: string;
  success: string;
  successMuted: string;
  warning: string;
  warningMuted: string;
  /** Alias of accentCyan for informational chips */
  info: string;
  infoMuted: string;
  /** Tab / chrome surfaces (dark navy even in light mode for brand continuity) */
  tabBarBackground: string;
  tabBarBorder: string;
  tabBarInactive: string;
  overlay: string;
}

export const lightColors: ColorTokens = {
  background: '#f4f6f9',
  surface: '#ffffff',
  surfaceElevated: '#ffffff',
  surfaceMuted: '#e8edf4',
  border: '#d5dde8',
  borderStrong: '#b8c4d4',
  text: '#0a0c10',
  textMuted: '#5b6b7c',
  primary: brand.lime600,
  primaryPressed: '#65a30d',
  primaryText: '#0a0c10',
  primaryMuted: 'rgba(132, 204, 22, 0.16)',
  accentCyan: brand.cyan600,
  accentCyanMuted: 'rgba(8, 145, 178, 0.14)',
  accentAi: brand.ai600,
  accentAiMuted: 'rgba(147, 51, 234, 0.12)',
  danger: '#dc2626',
  dangerMuted: 'rgba(220, 38, 38, 0.10)',
  success: '#65a30d',
  successMuted: 'rgba(132, 204, 22, 0.14)',
  warning: '#d97706',
  warningMuted: 'rgba(217, 119, 6, 0.12)',
  info: brand.cyan600,
  infoMuted: 'rgba(8, 145, 178, 0.14)',
  tabBarBackground: '#0e1218',
  tabBarBorder: '#252d3a',
  tabBarInactive: '#9aa8b8',
  overlay: 'rgba(10, 12, 16, 0.45)',
};

export const darkColors: ColorTokens = {
  background: '#0a0c10',
  surface: '#12171f',
  surfaceElevated: '#171d27',
  surfaceMuted: '#1a222e',
  border: '#252d3a',
  borderStrong: '#334155',
  text: '#f5f7fa',
  textMuted: '#8b949e',
  primary: brand.lime500,
  primaryPressed: brand.lime400,
  primaryText: '#0a0c10',
  primaryMuted: 'rgba(163, 230, 53, 0.14)',
  accentCyan: brand.cyan400,
  accentCyanMuted: 'rgba(34, 211, 238, 0.12)',
  accentAi: brand.ai400,
  accentAiMuted: 'rgba(192, 132, 252, 0.14)',
  danger: '#f87171',
  dangerMuted: 'rgba(248, 113, 113, 0.12)',
  success: brand.lime500,
  successMuted: 'rgba(163, 230, 53, 0.12)',
  warning: '#fbbf24',
  warningMuted: 'rgba(251, 191, 36, 0.12)',
  info: brand.cyan400,
  infoMuted: 'rgba(34, 211, 238, 0.12)',
  tabBarBackground: '#080a0e',
  tabBarBorder: '#252d3a',
  tabBarInactive: '#8b949e',
  overlay: 'rgba(0, 0, 0, 0.55)',
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
