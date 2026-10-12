import { Href, router } from 'expo-router';
import { Pressable, StyleSheet, Text } from 'react-native';

import { brand } from '@/src/theme/tokens';

export function CoachHomeEntry() {
  return (
    <Pressable
      testID="coach-entry"
      accessibilityRole="button"
      accessibilityLabel="Open AI Coach"
      onPress={() => router.push('/coach' as Href)}
      style={styles.entry}>
      <Text style={styles.label}>AI Coach</Text>
      <Text style={styles.detail}>Explains your UAP state. It does not calculate readiness.</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  entry: {
    borderWidth: 1,
    borderColor: brand.ai500,
    borderRadius: 12,
    padding: 12,
    gap: 4,
  },
  label: { color: brand.ai500, fontWeight: '700', fontSize: 16 },
  detail: { color: brand.ai400, fontSize: 13 },
});
