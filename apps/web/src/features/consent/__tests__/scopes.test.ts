import { describe, expect, it } from 'vitest';

import { CONSENT_SCOPE_CATALOG, consentScopeInfo } from '@/features/consent/models/scopes';
import { CONSENT_SCOPES } from '@/features/consent/models/schemas';

describe('consent scopes catalog', () => {
  it('covers every canonical scope with human-readable copy', () => {
    expect(CONSENT_SCOPE_CATALOG.map((entry) => entry.scope)).toEqual([...CONSENT_SCOPES]);
    for (const entry of CONSENT_SCOPE_CATALOG) {
      expect(entry.label.trim().length).toBeGreaterThan(0);
      expect(entry.description.trim().length).toBeGreaterThan(10);
      expect(entry.description.toLowerCase()).not.toMatch(/hipaa|ferpa|coppa|compliant/);
    }
  });

  it('resolves known scopes from the catalog', () => {
    expect(consentScopeInfo('READINESS_SCORE').label).toBe('Readiness score');
  });

  it('falls back for unknown scope strings', () => {
    const info = consentScopeInfo('CUSTOM_FUTURE_SCOPE');
    expect(info.label).toBe('Custom Future Scope');
    expect(info.description.length).toBeGreaterThan(0);
  });
});
