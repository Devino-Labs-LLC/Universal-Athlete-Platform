/**
 * UAP mobile semantic theme — aligned with Web Athlete Readiness / UAP tokens
 * (`apps/web/src/styles/tokens/_tokens.scss`), expressed for React Native.
 *
 * Brand roles: lime = primary action; cyan = nav/selection/info/focus;
 * purple/AI = reserved (defined, unused on current athlete surfaces).
 * Navy is the cool-neutral chrome family (no warm cream SaaS palette).
 */

import type { TextStyle, ViewStyle } from 'react-native';

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  /** Web `--uap-space-5` (20px) — optional mid step between lg and xl */
  xl20: 20,
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

/** Native type scale (points). System UI font; Manrope remains web-only. */
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

/**
 * Brand scales shared across themes (exact Web V1 / UAP hex values).
 * AI purple is reserved for future AI concepts only.
 */
export const brand = {
  lime400: '#bef264',
  lime500: '#a3e635',
  lime600: '#84cc16',
  cyan400: '#22d3ee',
  cyan500: '#06b6d4',
  cyan600: '#0891b2',
  /** Cool navy family — Web sidebar / mobile tab chrome */
  navy950: '#080a0e',
  navy900: '#0a0c10',
  navy850: '#0e1218',
  ai400: '#c084fc',
  ai500: '#a855f7',
  ai600: '#9333ea',
} as const;

/** Soft elevation — RN-friendly; mirrors Web `--uap-shadow-*` intent. */
export const shadows = {
  sm: {
    shadowColor: brand.navy900,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 2,
    elevation: 1,
  } satisfies ViewStyle,
  md: {
    shadowColor: brand.navy900,
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 20,
    elevation: 4,
  } satisfies ViewStyle,
  smDark: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.25,
    shadowRadius: 1,
    elevation: 1,
  } satisfies ViewStyle,
  mdDark: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.35,
    shadowRadius: 28,
    elevation: 6,
  } satisfies ViewStyle,
} as const;

export interface ColorTokens {
  // --- Surfaces ---
  background: string;
  surface: string;
  surfaceElevated: string;
  surfaceMuted: string;
  /** Alias of surfaceMuted */
  surfaceSubtle: string;

  // --- Borders ---
  border: string;
  borderStrong: string;
  /** Alias of border */
  divider: string;

  // --- Text (canonical + aliases) ---
  /** @deprecated Prefer textPrimary — kept for existing call sites */
  text: string;
  textPrimary: string;
  /** @deprecated Prefer textSecondary — kept for existing call sites */
  textMuted: string;
  textSecondary: string;
  /** On lime / filled primary controls */
  textInverse: string;

  // --- Brand / actions (lime primary, cyan secondary) ---
  /** @deprecated Prefer brandPrimary / actionPrimary */
  primary: string;
  primaryPressed: string;
  primaryText: string;
  primaryMuted: string;
  brandPrimary: string;
  brandSecondary: string;
  actionPrimary: string;
  actionPrimaryPressed: string;
  /** Secondary control fill (muted surface) */
  actionSecondary: string;
  focus: string;

  // --- Accents ---
  /** Cyan — navigation / selection / information */
  accentCyan: string;
  accentCyanMuted: string;
  /** Reserved for future AI concepts only — do not use on athlete surfaces */
  accentAi: string;
  accentAiMuted: string;

  // --- State ---
  danger: string;
  dangerMuted: string;
  /** Label on filled danger controls */
  dangerText: string;
  success: string;
  successMuted: string;
  warning: string;
  warningMuted: string;
  info: string;
  infoMuted: string;

  // --- Chrome ---
  /** Tab / chrome surfaces (dark navy even in light mode for brand continuity) */
  tabBarBackground: string;
  tabBarBorder: string;
  tabBarInactive: string;
  /** Selected tab / active chrome label (web sidebar-active-text) */
  tabBarActive: string;
  overlay: string;
}

type ColorCore = Omit<
  ColorTokens,
  | 'surfaceSubtle'
  | 'divider'
  | 'textPrimary'
  | 'textSecondary'
  | 'textInverse'
  | 'brandPrimary'
  | 'brandSecondary'
  | 'actionPrimary'
  | 'actionPrimaryPressed'
  | 'actionSecondary'
  | 'focus'
  | 'dangerText'
  | 'tabBarActive'
>;

function withSemantics(core: ColorCore, extras: { textInverse: string; dangerText: string; tabBarActive: string }): ColorTokens {
  return {
    ...core,
    surfaceSubtle: core.surfaceMuted,
    divider: core.border,
    textPrimary: core.text,
    textSecondary: core.textMuted,
    textInverse: extras.textInverse,
    brandPrimary: core.primary,
    brandSecondary: core.accentCyan,
    actionPrimary: core.primary,
    actionPrimaryPressed: core.primaryPressed,
    actionSecondary: core.surfaceMuted,
    focus: core.accentCyan,
    dangerText: extras.dangerText,
    tabBarActive: extras.tabBarActive,
  };
}

const lightCore: ColorCore = {
  background: '#f4f6f9',
  surface: '#ffffff',
  surfaceElevated: '#ffffff',
  surfaceMuted: '#e8edf4',
  border: '#d5dde8',
  borderStrong: '#b8c4d4',
  text: brand.navy900,
  textMuted: '#5b6b7c',
  primary: brand.lime600,
  primaryPressed: '#65a30d',
  primaryText: brand.navy900,
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
  tabBarBackground: brand.navy850,
  tabBarBorder: '#252d3a',
  tabBarInactive: '#9aa8b8',
  overlay: 'rgba(10, 12, 16, 0.45)',
};

const darkCore: ColorCore = {
  background: brand.navy900,
  surface: '#12171f',
  surfaceElevated: '#171d27',
  surfaceMuted: '#1a222e',
  border: '#252d3a',
  borderStrong: '#334155',
  text: '#f5f7fa',
  textMuted: '#8b949e',
  primary: brand.lime500,
  primaryPressed: brand.lime400,
  primaryText: brand.navy900,
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
  tabBarBackground: brand.navy950,
  tabBarBorder: '#252d3a',
  tabBarInactive: '#8b949e',
  overlay: 'rgba(0, 0, 0, 0.55)',
};

export const lightColors: ColorTokens = withSemantics(lightCore, {
  textInverse: brand.navy900,
  dangerText: '#ffffff',
  tabBarActive: '#f8fafc',
});

export const darkColors: ColorTokens = withSemantics(darkCore, {
  textInverse: brand.navy900,
  dangerText: '#ffffff',
  tabBarActive: '#f5f7fa',
});

export type ThemeMode = 'light' | 'dark';
/** Matches Web `ThemePreference` — system follows OS appearance. */
export type ThemePreference = 'light' | 'dark' | 'system';

export interface AppTheme {
  mode: ThemeMode;
  colors: ColorTokens;
  spacing: typeof spacing;
  radius: typeof radius;
  typography: typeof typography;
  shadows: {
    sm: ViewStyle;
    md: ViewStyle;
  };
}

export function createTheme(mode: ThemeMode): AppTheme {
  return {
    mode,
    colors: mode === 'dark' ? darkColors : lightColors,
    spacing,
    radius,
    typography,
    shadows: {
      sm: mode === 'dark' ? shadows.smDark : shadows.sm,
      md: mode === 'dark' ? shadows.mdDark : shadows.md,
    },
  };
}

export function resolveThemeMode(
  preference: ThemePreference,
  systemScheme: string | null | undefined,
): ThemeMode {
  if (preference === 'light' || preference === 'dark') {
    return preference;
  }
  return systemScheme === 'dark' ? 'dark' : 'light';
}

/** Eyebrow / label text style helper for token consumers. */
export function eyebrowTextStyle(theme: AppTheme): TextStyle {
  return {
    fontSize: theme.typography.eyebrow,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.colors.textSecondary,
  };
}
