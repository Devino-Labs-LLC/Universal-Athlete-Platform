import { Stack } from 'expo-router';

export default function TrainingStackLayout() {
  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="create-plan" options={{ title: 'Personal plan' }} />
    </Stack>
  );
}
