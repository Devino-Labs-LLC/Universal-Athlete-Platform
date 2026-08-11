import { Stack } from 'expo-router';

export default function PerformanceStackLayout() {
  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="index" options={{ title: 'Performance' }} />
      <Stack.Screen name="records" options={{ title: 'Personal Records' }} />
      <Stack.Screen name="load" options={{ title: 'Training Load' }} />
      <Stack.Screen
        name="exercises/[exercisePerformanceKey]"
        options={{ title: 'Exercise Performance' }}
      />
    </Stack>
  );
}
