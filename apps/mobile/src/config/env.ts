export type AppEnvironment = 'development' | 'staging' | 'production';

const DEVELOPMENT_DEFAULT_API_BASE_URL = 'http://127.0.0.1:8080';

export interface AppConfig {
  environment: AppEnvironment;
  apiBaseUrl: string;
}

function parseEnvironment(raw: string | undefined): AppEnvironment {
  if (raw === 'development' || raw === 'staging' || raw === 'production') {
    return raw;
  }
  return 'development';
}

function normalizeBaseUrl(url: string): string {
  return url.endsWith('/') ? url.slice(0, -1) : url;
}

export function loadAppConfig(
  env: Record<string, string | undefined> = process.env,
): AppConfig {
  const environment = parseEnvironment(env.EXPO_PUBLIC_UAP_ENV);
  const configuredUrl = env.EXPO_PUBLIC_UAP_API_BASE_URL?.trim();

  if (configuredUrl) {
    return {
      environment,
      apiBaseUrl: normalizeBaseUrl(configuredUrl),
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
