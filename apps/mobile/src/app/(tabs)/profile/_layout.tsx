import { Stack } from 'expo-router';

export default function ProfileStackLayout() {
  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="index" options={{ title: 'Profile' }} />
      <Stack.Screen name="environments/index" options={{ title: 'Training Environments' }} />
      <Stack.Screen name="environments/create" options={{ title: 'Create Environment' }} />
      <Stack.Screen
        name="environments/[environmentId]/index"
        options={{ title: 'Environment' }}
      />
      <Stack.Screen
        name="environments/[environmentId]/edit"
        options={{ title: 'Edit Environment' }}
      />
    </Stack>
  );
}
