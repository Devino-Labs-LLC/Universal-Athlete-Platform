#!/usr/bin/env node
/**
 * HTTP cookie/CSRF/refresh smoke against a running UAP backend.
 *
 * This validates the backend auth contract the Expo native client relies on.
 * Native CookieManager behavior still requires an Expo Dev Build on simulator/device.
 *
 * Usage:
 *   node scripts/cookie-auth-smoke.mjs [baseUrl]
 */
import axios from 'axios';
import { CookieJar } from 'tough-cookie';
import { wrapper } from 'axios-cookiejar-support';

const baseURL = process.argv[2] ?? process.env.EXPO_PUBLIC_UAP_API_BASE_URL ?? 'http://127.0.0.1:8080';
const password = 'SecurePass123!';
const email = `m1r.smoke.${Date.now()}@example.com`;

function getCookie(jar, name) {
  const cookies = jar.getCookiesSync(baseURL);
  return cookies.find((c) => c.key === name)?.value;
}

function requireCookie(jar, name) {
  const value = getCookie(jar, name);
  if (!value) {
    throw new Error(`Missing cookie: ${name}`);
  }
  return value;
}

async function main() {
  const jar = new CookieJar();
  const client = wrapper(
    axios.create({
      baseURL,
      jar,
      withCredentials: true,
      validateStatus: () => true,
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    }),
  );

  console.log(`Smoke base URL: ${baseURL}`);

  const register = await client.post('/api/v1/identity/register', { email, password });
  if (register.status !== 201) {
    throw new Error(`register failed: ${register.status} ${JSON.stringify(register.data)}`);
  }
  console.log('✓ register');

  // Dev notifier is in-memory; for a live server we may not have the token.
  // Attempt login — if EMAIL_NOT_VERIFIED, document and continue with verified path if available.
  let login = await client.post('/api/v1/identity/login', { email, password });
  if (login.status === 403 && login.data?.code === 'EMAIL_NOT_VERIFIED') {
    console.log('! email not verified on live server — cannot complete login without verification token');
    console.log('  Contract check so far: register endpoint reachable.');
    process.exit(2);
  }
  if (login.status !== 200) {
    throw new Error(`login failed: ${login.status} ${JSON.stringify(login.data)}`);
  }
  console.log('✓ login');

  requireCookie(jar, 'uap_at');
  requireCookie(jar, 'uap_rt');
  console.log('✓ auth cookies present (uap_at, uap_rt)');

  // Seed CSRF via authenticated GET if needed
  const me = await client.get('/api/v1/identity/me');
  if (me.status !== 200) {
    throw new Error(`/me failed: ${me.status}`);
  }
  console.log('✓ GET /identity/me');

  const xsrf = getCookie(jar, 'XSRF-TOKEN');
  if (!xsrf) {
    throw new Error('XSRF-TOKEN cookie missing after authenticated GET');
  }
  console.log('✓ XSRF-TOKEN cookie present');

  const refresh = await client.post(
    '/api/v1/identity/refresh',
    undefined,
    { headers: { 'X-XSRF-TOKEN': xsrf } },
  );
  if (refresh.status !== 204) {
    throw new Error(`refresh failed: ${refresh.status} ${JSON.stringify(refresh.data)}`);
  }
  console.log('✓ refresh (CSRF-protected)');

  const meAfter = await client.get('/api/v1/identity/me');
  if (meAfter.status !== 200) {
    throw new Error(`/me after refresh failed: ${meAfter.status}`);
  }
  console.log('✓ /me after refresh');

  const xsrf2 = requireCookie(jar, 'XSRF-TOKEN');
  const logout = await client.post(
    '/api/v1/identity/logout',
    undefined,
    { headers: { 'X-XSRF-TOKEN': xsrf2 } },
  );
  if (logout.status !== 204) {
    throw new Error(`logout failed: ${logout.status} ${JSON.stringify(logout.data)}`);
  }
  console.log('✓ logout');

  const meGone = await client.get('/api/v1/identity/me');
  if (meGone.status !== 401) {
    throw new Error(`expected 401 after logout, got ${meGone.status}`);
  }
  console.log('✓ /me unauthorized after logout');

  // Login again for training facades
  login = await client.post('/api/v1/identity/login', { email, password });
  if (login.status !== 200) {
    throw new Error(`re-login failed: ${login.status}`);
  }
  const bootstrap = await client.get('/api/v1/training/client/bootstrap');
  if (bootstrap.status !== 200 || bootstrap.data?.clientContractVersion !== 'V1') {
    throw new Error(`bootstrap failed: ${bootstrap.status} ${JSON.stringify(bootstrap.data)}`);
  }
  console.log('✓ bootstrap V1');

  const today = await client.get('/api/v1/training/client/today');
  if (today.status !== 200) {
    throw new Error(`today failed: ${today.status}`);
  }
  console.log('✓ today facade');

  console.log('\nCOOKIE AUTH SMOKE PASSED (HTTP jar / tough-cookie)');
  console.log('Native Expo CookieManager still requires Dev Build validation on simulator/device.');
}

main().catch((error) => {
  console.error('SMOKE FAILED:', error.message ?? error);
  process.exit(1);
});
