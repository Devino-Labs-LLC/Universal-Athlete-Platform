// Flat ESLint config for Expo SDK 57 (see https://docs.expo.dev/guides/using-eslint/).
// Keep this file checked in so `pnpm lint` / `pnpm mobile:lint` never prompt to install deps.
const { defineConfig, globalIgnores } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  globalIgnores([
    'dist/**',
    '.expo/**',
    'node_modules/**',
    'coverage/**',
    'android/**',
    'ios/**',
  ]),
  expoConfig,
  {
    rules: {
      // Auth/onboarding/bootstrap providers restore session state on mount via
      // async loaders invoked from effects. Same class of false positive the web
      // app disables (`react-hooks/set-state-in-effect`).
      'react-hooks/set-state-in-effect': 'off',
    },
  },
]);
