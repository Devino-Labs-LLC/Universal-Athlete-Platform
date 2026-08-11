import { z } from 'zod';

export const PASSWORD_MIN_LENGTH = 12;
export const PASSWORD_MAX_LENGTH = 128;

const upperCasePattern = /[A-Z]/;
const lowerCasePattern = /[a-z]/;
const digitPattern = /\d/;
const specialPattern = /[^A-Za-z0-9]/;

export function passwordPolicyViolations(password: string): string[] {
  const violations: string[] = [];

  if (password.length < PASSWORD_MIN_LENGTH) {
    violations.push(`at least ${PASSWORD_MIN_LENGTH} characters`);
  }
  if (password.length > PASSWORD_MAX_LENGTH) {
    violations.push(`no more than ${PASSWORD_MAX_LENGTH} characters`);
  }
  if (!upperCasePattern.test(password)) {
    violations.push('an uppercase letter');
  }
  if (!lowerCasePattern.test(password)) {
    violations.push('a lowercase letter');
  }
  if (!digitPattern.test(password)) {
    violations.push('a digit');
  }
  if (!specialPattern.test(password)) {
    violations.push('a special character');
  }

  return violations;
}

export function isPasswordPolicyCompliant(password: string): boolean {
  return passwordPolicyViolations(password).length === 0;
}

export function passwordPolicyMessage(password: string): string | null {
  const violations = passwordPolicyViolations(password);
  if (violations.length === 0) {
    return null;
  }
  return `Password must include ${violations.join(', ')}.`;
}

const passwordFieldSchema = z
  .string()
  .min(PASSWORD_MIN_LENGTH, `Password must be at least ${PASSWORD_MIN_LENGTH} characters`)
  .max(PASSWORD_MAX_LENGTH, `Password must be at most ${PASSWORD_MAX_LENGTH} characters`)
  .superRefine((value, ctx) => {
    const message = passwordPolicyMessage(value);
    if (message) {
      ctx.addIssue({ code: 'custom', message });
    }
  });

export const meResponseSchema = z.object({
  accountId: z.string(),
  email: z.string().email(),
  status: z.string(),
  emailVerifiedAt: z.string().nullable(),
});

export type MeResponse = z.infer<typeof meResponseSchema>;

export const loginRequestSchema = z.object({
  email: z.string().trim().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export type LoginRequest = z.infer<typeof loginRequestSchema>;

export const loginResponseSchema = z.object({
  accountId: z.string(),
  status: z.string(),
});

export type LoginResponse = z.infer<typeof loginResponseSchema>;

export const registerRequestSchema = z.object({
  email: z.string().trim().email('Enter a valid email address'),
  password: passwordFieldSchema,
});

export type RegisterRequest = z.infer<typeof registerRequestSchema>;

export const registerResponseSchema = z.object({
  accountId: z.string(),
  email: z.string().email(),
  status: z.string(),
});

export type RegisterResponse = z.infer<typeof registerResponseSchema>;

export const verifyEmailRequestSchema = z.object({
  token: z.string().trim().min(1, 'Verification token is required'),
});

export type VerifyEmailRequest = z.infer<typeof verifyEmailRequestSchema>;
