import type { ExpoConfig, ConfigContext } from 'expo/config';

export default ({ config }: ConfigContext): ExpoConfig => {
  // Read at evaluation time so EAS profile env (and tests) control ATS/cleartext.
  const isDevelopment = process.env.EXPO_PUBLIC_UAP_ENV === 'development';

  return {
    ...config,
    owner: 'jon204ds-team',
    extra: {
      ...config.extra,
      eas: {
        projectId: 'b9166022-2b56-4047-9a55-c0971e322fb3',
      },
    },
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
              // Allow LAN-device → host-machine HTTP during local Dev Client work.
              NSAllowsLocalNetworking: true,
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
      // C2: Health Connect READ declarations (WRITE not requested).
      permissions: [
        'android.permission.health.READ_SLEEP',
        'android.permission.health.READ_RESTING_HEART_RATE',
        'android.permission.health.READ_HEART_RATE_VARIABILITY',
        'android.permission.health.READ_STEPS',
        'android.permission.health.READ_ACTIVE_CALORIES_BURNED',
        'android.permission.health.READ_EXERCISE',
      ],
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
      'expo-font',
      'expo-web-browser',
      // C1: react-native-health Expo config plugin — READ usage only (no clinical records).
      // Requires an iOS development / EAS build; not available in Expo Go.
      [
        'react-native-health',
        {
          isClinicalDataEnabled: false,
          healthSharePermission:
            'Athlete Readiness reads sleep, resting heart rate, heart-rate variability, activity energy, steps, and workouts from Apple Health to store connected evidence. It does not write health data.',
          healthUpdatePermission:
            'Athlete Readiness does not write to Apple Health. This string is required by the HealthKit capability configuration.',
        },
      ],
      // C2: react-native-health-connect Expo config plugin — READ usage only.
      // Requires an Android development / EAS build; not available in Expo Go.
      'react-native-health-connect',
      [
        'expo-build-properties',
        {
          android: {
            minSdkVersion: 26,
          },
        },
      ],
    ],
    experiments: {
      typedRoutes: true,
    },
  };
};
