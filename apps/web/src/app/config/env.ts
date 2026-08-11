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
    return (
      host === 'localhost' ||
      host === '127.0.0.1' ||
      host === '[::1]' ||
      host === '::1' ||
      host === '0.0.0.0' ||
      host === '10.0.2.2'
    );
  } catch {
    return /localhost|127\.0\.0\.1|\[?::1\]?|0\.0\.0\.0|10\.0\.2\.2/i.test(url);
  }
}

export function loadAppConfig(
  env: Record<string, string | undefined> = import.meta.env as Record<string, string | undefined>,
  options?: { releaseBuild?: boolean },
): AppConfig {
  const releaseBuild = options?.releaseBuild ?? isReleaseBuild();
  const environment = parseEnvironment(env.VITE_UAP_ENV, releaseBuild);

  // Release artifacts must not ship labeled as development (would skip URL guards).
  if (releaseBuild && environment === 'development') {
    throw new Error(
      'VITE_UAP_ENV=development is not allowed in release builds; use staging or production',
    );
  }

  const configuredUrl = env.VITE_UAP_API_BASE_URL?.trim();

  if (configuredUrl) {
    const apiBaseUrl = normalizeBaseUrl(configuredUrl);
    let parsedUrl: URL | null = null;
    if (environment !== 'development') {
      try {
        parsedUrl = new URL(apiBaseUrl);
      } catch {
        throw new Error(
          `A valid absolute API URL is required when VITE_UAP_ENV=${environment}`,
        );
      }
    }
    if (environment !== 'development' && isLocalhostUrl(apiBaseUrl)) {
      throw new Error(
        `Localhost API URLs are not allowed when VITE_UAP_ENV=${environment}`,
      );
    }
    if (parsedUrl && parsedUrl.protocol !== 'https:') {
      throw new Error(
        `HTTPS API URL is required when VITE_UAP_ENV=${environment}`,
      );
    }
    if (parsedUrl && (parsedUrl.username || parsedUrl.password)) {
      throw new Error('VITE_UAP_API_BASE_URL must not contain embedded credentials');
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
