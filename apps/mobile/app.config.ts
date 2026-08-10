import type { ExpoConfig, ConfigContext } from 'expo/config';

const isDevelopment = process.env.EXPO_PUBLIC_UAP_ENV === 'development';

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Universal Athlete',
  slug: 'uap-mobile',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'uap',
  userInterfaceStyle: 'automatic',
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'com.devinolabs.uap',
    infoPlist: isDevelopment
      ? {
          NSAppTransportSecurity: {
            NSExceptionDomains: {
              localhost: {
                NSExceptionAllowsInsecureHTTPLoads: true,
                NSIncludesSubdomains: true,
              },
              '127.0.0.1': {
                NSExceptionAllowsInsecureHTTPLoads: true,
                NSIncludesSubdomains: true,
              },
            },
          },
        }
      : undefined,
  },
  android: {
    adaptiveIcon: {
      backgroundColor: '#0F172A',
      foregroundImage: './assets/images/android-icon-foreground.png',
      backgroundImage: './assets/images/android-icon-background.png',
      monochromeImage: './assets/images/android-icon-monochrome.png',
    },
    package: 'com.devinolabs.uap',
    predictiveBackGestureEnabled: false,
    ...(isDevelopment ? { usesCleartextTraffic: true } : {}),
  },
  web: {
    bundler: 'metro',
    output: 'static',
    favicon: './assets/images/favicon.png',
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        image: './assets/images/splash-icon.png',
        resizeMode: 'contain',
        backgroundColor: '#0F172A',
      },
    ],
    'expo-secure-store',
    'expo-dev-client',
  ],
  experiments: {
    typedRoutes: true,
  },
});
