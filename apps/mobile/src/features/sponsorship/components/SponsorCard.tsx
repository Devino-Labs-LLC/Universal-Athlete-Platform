import { Image, Linking, Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import type { SponsorshipPlacement } from '@/src/features/sponsorship/api/sponsorshipApi';

interface SponsorCardProps {
  placement: SponsorshipPlacement;
  onVisit?: () => void;
}

export function SponsorCard({ placement, onVisit }: SponsorCardProps) {
  const theme = useAppTheme();
  return (
    <View
      testID="sponsor-card"
      accessibilityLabel="Sponsored partner content"
      style={[styles.card, { borderColor: theme.colors.border, backgroundColor: theme.colors.surface }]}>
      <Text style={[styles.label, { color: theme.colors.textSecondary }]}>{placement.label}</Text>
      <Text style={[styles.sponsor, { color: theme.colors.textSecondary }]}>{placement.sponsorDisplayName}</Text>
      <Text style={[styles.headline, { color: theme.colors.textPrimary }]}>{placement.headline}</Text>
      <Text style={[styles.body, { color: theme.colors.textSecondary }]}>{placement.body}</Text>
      {placement.creativeUrl ? (
        <Image
          testID="sponsor-creative"
          source={{ uri: placement.creativeUrl }}
          style={styles.creative}
          accessibilityIgnoresInvertColors
        />
      ) : null}
      <Pressable
        accessibilityRole="link"
        accessibilityLabel={placement.ctaLabel}
        onPress={() => {
          onVisit?.();
          void Linking.openURL(placement.destinationUrl);
        }}>
        <Text style={[styles.cta, { color: theme.colors.textPrimary }]}>{placement.ctaLabel}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: 12,
    padding: 12,
    gap: 6,
  },
  label: { fontSize: 11, fontWeight: '700', letterSpacing: 0.8, textTransform: 'uppercase' },
  sponsor: { fontSize: 13, fontWeight: '600' },
  headline: { fontSize: 16, fontWeight: '600' },
  body: { fontSize: 14 },
  creative: { width: 160, height: 48, resizeMode: 'contain' },
  cta: { fontSize: 15, fontWeight: '700', textDecorationLine: 'underline' },
});
