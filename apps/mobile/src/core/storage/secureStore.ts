import * as SecureStore from 'expo-secure-store';

const ALLOWED_KEYS = new Set(['uap:lastEmail']);

export async function getSecureItem(key: string): Promise<string | null> {
  if (!ALLOWED_KEYS.has(key)) {
    throw new Error(`Secure store key not allowed: ${key}`);
  }
  return SecureStore.getItemAsync(key);
}

export async function setSecureItem(key: string, value: string): Promise<void> {
  if (!ALLOWED_KEYS.has(key)) {
    throw new Error(`Secure store key not allowed: ${key}`);
  }
  await SecureStore.setItemAsync(key, value);
}

export async function deleteSecureItem(key: string): Promise<void> {
  if (!ALLOWED_KEYS.has(key)) {
    throw new Error(`Secure store key not allowed: ${key}`);
  }
  await SecureStore.deleteItemAsync(key);
}
