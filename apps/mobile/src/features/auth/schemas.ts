import { z } from 'zod';

export const meResponseSchema = z.object({
  accountId: z.string(),
  email: z.string().email(),
  status: z.string(),
  emailVerifiedAt: z.string().nullable(),
});

export type MeResponse = z.infer<typeof meResponseSchema>;

export const loginRequestSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export type LoginRequest = z.infer<typeof loginRequestSchema>;

export const loginResponseSchema = z.object({
  accountId: z.string(),
  status: z.string(),
});

export type LoginResponse = z.infer<typeof loginResponseSchema>;

export const registerRequestSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});

export type RegisterRequest = z.infer<typeof registerRequestSchema>;

export const registerResponseSchema = z.object({
  accountId: z.string(),
  email: z.string().email(),
  status: z.string(),
});

export type RegisterResponse = z.infer<typeof registerResponseSchema>;

export const verifyEmailRequestSchema = z.object({
  token: z.string().min(1),
});

export type VerifyEmailRequest = z.infer<typeof verifyEmailRequestSchema>;
