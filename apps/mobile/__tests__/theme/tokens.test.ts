import { createTheme, darkColors, lightColors, brand } from '@/src/app/theme/tokens';

describe('UAP mobile theme tokens', () => {
  it('maps dark mode to Web V1 navy / lime / cyan semantics', () => {
    const theme = createTheme('dark');
    expect(theme.colors.background).toBe('#0a0c10');
    expect(theme.colors.surface).toBe('#12171f');
    expect(theme.colors.primary).toBe(brand.lime500);
    expect(theme.colors.accentCyan).toBe(brand.cyan400);
    expect(theme.colors.info).toBe(theme.colors.accentCyan);
    expect(theme.colors.primaryText).toBe('#0a0c10');
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
});
