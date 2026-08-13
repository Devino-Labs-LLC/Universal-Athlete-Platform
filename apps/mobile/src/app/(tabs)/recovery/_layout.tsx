import { Stack } from 'expo-router';

export default function RecoveryStackLayout() {
  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="index" options={{ title: 'Recovery' }} />
      <Stack.Screen name="check-in" options={{ title: 'Check-in' }} />
      <Stack.Screen name="history" options={{ title: 'History' }} />
      <Stack.Screen name="analytics" options={{ title: 'Analytics' }} />
      <Stack.Screen name="readiness/[assessmentId]" options={{ title: 'Readiness' }} />
      <Stack.Screen name="guidance/[recommendationId]" options={{ title: 'Guidance' }} />
    </Stack>
  );
}
