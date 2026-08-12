import fs from 'node:fs';
import path from 'node:path';

/**
 * Regression guard: auth session must stay cookie/XSRF based.
 * SecureStore may exist for unrelated secrets, but must not store auth JWTs.
 */
describe('no SecureStore auth-token regression', () => {
  it('does not write auth tokens into SecureStore from auth/api/session modules', () => {
    const roots = [
      path.join(__dirname, '../../src/providers'),
      path.join(__dirname, '../../src/features/auth'),
      path.join(__dirname, '../../src/core/api'),
    ];
    const offenders: string[] = [];
    const authTokenPattern =
      /SecureStore\.(setItemAsync|getItemAsync|deleteItemAsync)|setSecureItem|getSecureItem/;
    const bannedKeys = /uap_at|uap_rt|accessToken|refreshToken|idToken|jwt/i;

    for (const root of roots) {
      for (const file of walk(root)) {
        const source = fs.readFileSync(file, 'utf8');
        if (!authTokenPattern.test(source) && !/secureStore/i.test(source)) {
          continue;
        }
        if (bannedKeys.test(source) && /secureStore|SecureStore/i.test(source)) {
          offenders.push(path.relative(path.join(__dirname, '../..'), file));
        }
      }
    }

    expect(offenders).toEqual([]);
  });
});

function walk(dir: string): string[] {
  if (!fs.existsSync(dir)) {
    return [];
  }
  const out: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      out.push(...walk(full));
    } else if (/\.(ts|tsx)$/.test(entry.name)) {
      out.push(full);
    }
  }
  return out;
}
