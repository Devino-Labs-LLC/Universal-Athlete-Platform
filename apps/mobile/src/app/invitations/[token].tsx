import { useLocalSearchParams } from 'expo-router';

import { InvitationTokenScreen } from '@/src/features/organization/screens/InvitationTokenScreen';

function resolveTokenParam(value: string | string[] | undefined): string {
  if (typeof value === 'string') {
    return value;
  }
  if (Array.isArray(value) && typeof value[0] === 'string') {
    return value[0];
  }
  return '';
}

export default function InvitationTokenRoute() {
  const params = useLocalSearchParams<{ token?: string | string[] }>();
  const token = resolveTokenParam(params.token);
  return <InvitationTokenScreen token={token} />;
}
