import {
  brand,
  createTheme,
  darkColors,
  lightColors,
  resolveThemeMode,
} from '@/src/app/theme/tokens';

describe('UAP mobile theme tokens', () => {
  it('maps dark mode to Web V1 navy / lime / cyan semantics', () => {
    const theme = createTheme('dark');
    expect(theme.colors.background).toBe('#0a0c10');
    expect(theme.colors.surface).toBe('#12171f');
    expect(theme.colors.primary).toBe(brand.lime500);
    expect(theme.colors.accentCyan).toBe(brand.cyan400);
    expect(theme.colors.info).toBe(theme.colors.accentCyan);
    expect(theme.colors.primaryText).toBe('#0a0c10');
    expect(theme.colors.textInverse).toBe(brand.navy900);
    expect(theme.typography.metric).toBeGreaterThan(theme.typography.body);
    expect(theme.typography.eyebrow).toBeLessThan(theme.typography.caption);
  });

  it('maps light mode to the same semantic system with contrast-safe accents', () => {
    const theme = createTheme('light');
    expect(theme.colors.background).toBe('#f4f6f9');
    expect(theme.colors.primary).toBe(brand.lime600);
    expect(theme.colors.accentCyan).toBe(brand.cyan600);
    expect(theme.colors.tabBarBackground).toBe(lightColors.tabBarBackground);
    expect(darkColors.tabBarBackground).toMatch(/^#0/);
  });

  it('exposes semantic aliases that match canonical roles', () => {
    const light = createTheme('light');
    expect(light.colors.brandPrimary).toBe(light.colors.primary);
    expect(light.colors.brandSecondary).toBe(light.colors.accentCyan);
    expect(light.colors.actionPrimary).toBe(light.colors.primary);
    expect(light.colors.actionPrimaryPressed).toBe(light.colors.primaryPressed);
    expect(light.colors.textPrimary).toBe(light.colors.text);
    expect(light.colors.textSecondary).toBe(light.colors.textMuted);
    expect(light.colors.surfaceSubtle).toBe(light.colors.surfaceMuted);
    expect(light.colors.divider).toBe(light.colors.border);
    expect(light.colors.focus).toBe(light.colors.accentCyan);
    expect(light.colors.dangerText).toBe('#ffffff');
  });

  it('resolves theme preference against system scheme', () => {
    expect(resolveThemeMode('light', 'dark')).toBe('light');
    expect(resolveThemeMode('dark', 'light')).toBe('dark');
    expect(resolveThemeMode('system', 'dark')).toBe('dark');
    expect(resolveThemeMode('system', 'light')).toBe('light');
    expect(resolveThemeMode('system', null)).toBe('light');
  });

  it('attaches mode-appropriate elevation shadows', () => {
    const light = createTheme('light');
    const dark = createTheme('dark');
    expect(light.shadows.sm.shadowOpacity).toBeLessThan(dark.shadows.sm.shadowOpacity ?? 1);
    expect(dark.shadows.md.elevation).toBeGreaterThanOrEqual(light.shadows.md.elevation ?? 0);
  });
});
