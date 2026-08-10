import { ApiClient } from '@/src/core/api/apiClient';
import {
  LoginRequest,
  loginResponseSchema,
  MeResponse,
  meResponseSchema,
  RegisterRequest,
  registerResponseSchema,
  VerifyEmailRequest,
} from '@/src/features/auth/schemas';

const ME_PATH = '/api/v1/identity/me';
const LOGIN_PATH = '/api/v1/identity/login';
const REGISTER_PATH = '/api/v1/identity/register';
const VERIFY_EMAIL_PATH = '/api/v1/identity/verify-email';
const LOGOUT_PATH = '/api/v1/identity/logout';

export async function fetchMe(client: ApiClient): Promise<MeResponse> {
  const response = await client.axios.get(ME_PATH);
  return meResponseSchema.parse(response.data);
}

export async function login(client: ApiClient, request: LoginRequest): Promise<MeResponse> {
  await client.axios.post(LOGIN_PATH, request);
  return fetchMe(client);
}

export async function register(client: ApiClient, request: RegisterRequest) {
  const response = await client.axios.post(REGISTER_PATH, request);
  return registerResponseSchema.parse(response.data);
}

export async function verifyEmail(client: ApiClient, request: VerifyEmailRequest): Promise<void> {
  await client.axios.post(VERIFY_EMAIL_PATH, request);
}

export async function logout(client: ApiClient): Promise<void> {
  await client.axios.post(LOGOUT_PATH);
}
