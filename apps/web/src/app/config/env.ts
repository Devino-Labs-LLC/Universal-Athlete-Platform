export type AppEnvironment = 'development' | 'staging' | 'production';

export interface AppConfig {
  environment: AppEnvironment;
  apiBaseUrl: string;
}

function isReleaseBuild(): boolean {
  return import.meta.env.PROD;
}

function parseEnvironment(
  raw: string | undefined,
  releaseBuild: boolean,
): AppEnvironment {
  if (raw === 'development' || raw === 'staging' || raw === 'production') {
    return raw;
  }

  if (releaseBuild) {
    throw new Error(
      'VITE_UAP_ENV must be explicitly set to development, staging, or production in release builds',
    );
  }

  return 'development';
}

function normalizeBaseUrl(url: string): string {
  return url.endsWith('/') ? url.slice(0, -1) : url;
}

function isLocalhostUrl(url: string): boolean {
  if (!url) {
    return false;
  }

  try {
    const host = new URL(url).hostname;
    return host === 'localhost' || host === '127.0.0.1' || host === '10.0.2.2';
  } catch {
    return /localhost|127\.0\.0\.1|10\.0\.2\.2/i.test(url);
  }
}

export function loadAppConfig(
  env: Record<string, string | undefined> = import.meta.env as Record<string, string | undefined>,
  options?: { releaseBuild?: boolean },
): AppConfig {
  const environment = parseEnvironment(
    env.VITE_UAP_ENV,
    options?.releaseBuild ?? isReleaseBuild(),
  );
  const configuredUrl = env.VITE_UAP_API_BASE_URL?.trim();

  if (configuredUrl) {
    const apiBaseUrl = normalizeBaseUrl(configuredUrl);
    if (environment !== 'development' && isLocalhostUrl(apiBaseUrl)) {
      throw new Error(
        `Localhost API URLs are not allowed when VITE_UAP_ENV=${environment}`,
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
      apiBaseUrl: '',
    };
  }

  throw new Error(
    `VITE_UAP_API_BASE_URL is required when VITE_UAP_ENV=${environment}`,
  );
}
