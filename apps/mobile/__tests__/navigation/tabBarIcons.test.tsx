import { readdirSync, statSync } from 'node:fs';
import path from 'node:path';

import {
  resolveTabIconName,
  TAB_ICON_MAP,
  TabRouteName,
} from '@/src/core/navigation/tabBarIcons';

/** Top-level entries Expo Router may register under (tabs). Helpers must not appear here. */
const ALLOWED_TABS_TOP_LEVEL = new Set([
  '_layout.tsx',
  'index.tsx',
  'training',
  'recovery',
  'performance',
  'profile',
]);

describe('bottom tab icon identities', () => {
  const routes: TabRouteName[] = [
    'index',
    'training',
    'recovery',
    'performance',
    'profile',
  ];

  it('maps every tab to a stable label and icon pair', () => {
    expect(Object.keys(TAB_ICON_MAP).sort()).toEqual([...routes].sort());
    expect(TAB_ICON_MAP.index.label).toBe('Home');
    expect(TAB_ICON_MAP.training.label).toBe('Training');
    expect(TAB_ICON_MAP.recovery.label).toBe('Recovery');
    expect(TAB_ICON_MAP.performance.label).toBe('Performance');
    expect(TAB_ICON_MAP.profile.label).toBe('Profile');
  });

  it('uses outline icons when unfocused and filled when focused', () => {
    for (const route of routes) {
      const focused = resolveTabIconName(route, true);
      const unfocused = resolveTabIconName(route, false);
      expect(focused).toBe(TAB_ICON_MAP[route].focused);
      expect(unfocused).toBe(TAB_ICON_MAP[route].unfocused);
      expect(String(unfocused)).toContain('outline');
      expect(String(focused)).not.toContain('outline');
    }
  });

  it('uses product-appropriate Ionicons glyphs', () => {
    expect(TAB_ICON_MAP.index.focused).toBe('home');
    expect(TAB_ICON_MAP.training.focused).toBe('barbell');
    expect(TAB_ICON_MAP.recovery.focused).toBe('heart');
    expect(TAB_ICON_MAP.performance.focused).toBe('stats-chart');
    expect(TAB_ICON_MAP.profile.focused).toBe('person');
  });
});

describe('Expo Router tabs route hygiene', () => {
  const tabsDir = path.resolve(__dirname, '../../src/app/(tabs)');

  it('only exposes the five product tabs at the (tabs) top level', () => {
    const entries = readdirSync(tabsDir);
    expect(new Set(entries)).toEqual(ALLOWED_TABS_TOP_LEVEL);

    for (const entry of entries) {
      const full = path.join(tabsDir, entry);
      if (entry.endsWith('.tsx') || entry.endsWith('.ts')) {
        expect(statSync(full).isFile()).toBe(true);
        expect(entry === '_layout.tsx' || entry === 'index.tsx').toBe(true);
      } else {
        expect(statSync(full).isDirectory()).toBe(true);
      }
    }
  });

  it('does not keep navigation helpers inside the route directory', () => {
    const entries = readdirSync(tabsDir);
    expect(entries).not.toContain('tabBarIcons.tsx');
    expect(entries.some((e) => e.toLowerCase().includes('icon'))).toBe(false);
  });
});
