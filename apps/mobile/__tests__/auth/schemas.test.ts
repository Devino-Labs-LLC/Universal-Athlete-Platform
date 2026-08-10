import {
  isPasswordPolicyCompliant,
  loginRequestSchema,
  passwordPolicyViolations,
  registerRequestSchema,
} from '@/src/features/auth/schemas';

describe('auth schemas', () => {
  it('requires register passwords to be at least 12 characters', () => {
    const result = registerRequestSchema.safeParse({
      email: 'athlete@example.com',
      password: 'short',
    });

    expect(result.success).toBe(false);
  });

  it('accepts register passwords that satisfy policy', () => {
    const result = registerRequestSchema.safeParse({
      email: 'athlete@example.com',
      password: 'ValidPass123!',
    });

    expect(result.success).toBe(true);
  });

  it('reports password policy violations', () => {
    expect(passwordPolicyViolations('alllower123!')).toContain('an uppercase letter');
    expect(isPasswordPolicyCompliant('ValidPass123!')).toBe(true);
  });

  it('validates login email and required password', () => {
    expect(loginRequestSchema.safeParse({ email: 'bad', password: '' }).success).toBe(false);
    expect(
      loginRequestSchema.safeParse({
        email: 'athlete@example.com',
        password: 'secret',
      }).success,
    ).toBe(true);
  });
});
