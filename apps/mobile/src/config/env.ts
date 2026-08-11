export type AppEnvironment = 'development' | 'staging' | 'production';

const DEVELOPMENT_DEFAULT_API_BASE_URL = 'http://127.0.0.1:8080';

export interface AppConfig {
  environment: AppEnvironment;
  apiBaseUrl: string;
}

function isReleaseBuild(): boolean {
  // Expo sets __DEV__ false for production/release bundles.
  return typeof __DEV__ !== 'undefined' ? __DEV__ === false : process.env.NODE_ENV === 'production';
}

function parseEnvironment(raw: string | undefined): AppEnvironment {
  if (raw === 'development' || raw === 'staging' || raw === 'production') {
    return raw;
  }

  if (isReleaseBuild()) {
    throw new Error(
      'EXPO_PUBLIC_UAP_ENV must be explicitly set to development, staging, or production in release builds',
    );
  }

  // Local Metro / Expo Go convenience only.
  return 'development';
}

function normalizeBaseUrl(url: string): string {
  return url.endsWith('/') ? url.slice(0, -1) : url;
}

function isLocalhostUrl(url: string): boolean {
  try {
    const host = new URL(url).hostname;
    return host === 'localhost' || host === '127.0.0.1' || host === '10.0.2.2';
  } catch {
    return /localhost|127\.0\.0\.1|10\.0\.2\.2/i.test(url);
  }
}

export function loadAppConfig(
  env: Record<string, string | undefined> = process.env,
): AppConfig {
  const environment = parseEnvironment(env.EXPO_PUBLIC_UAP_ENV);
  const configuredUrl = env.EXPO_PUBLIC_UAP_API_BASE_URL?.trim();

  if (configuredUrl) {
    const apiBaseUrl = normalizeBaseUrl(configuredUrl);
    if (environment !== 'development' && isLocalhostUrl(apiBaseUrl)) {
      throw new Error(
        `Localhost API URLs are not allowed when EXPO_PUBLIC_UAP_ENV=${environment}`,
      );
    }
    return {
      environment,
      apiBaseUrl,
    };
  }

  if (environment === 'development') {
    return {
      environment,
      apiBaseUrl: normalizeBaseUrl(DEVELOPMENT_DEFAULT_API_BASE_URL),
    };
  }

  throw new Error(
    `EXPO_PUBLIC_UAP_API_BASE_URL is required when EXPO_PUBLIC_UAP_ENV=${environment}`,
  );
}
