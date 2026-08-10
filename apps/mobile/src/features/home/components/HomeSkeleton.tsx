import { StyleSheet, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';

function SkeletonBlock({ height }: { height: number }) {
  const theme = useAppTheme();
  return (
    <View
      style={[
        styles.block,
        {
          height,
          backgroundColor: theme.colors.border,
        },
      ]}
    />
  );
}

export function HomeSkeleton() {
  return (
    <View testID="home-skeleton" style={styles.container}>
      <SkeletonBlock height={32} />
      <SkeletonBlock height={18} />
      <HomeCard>
        <SkeletonBlock height={20} />
        <SkeletonBlock height={48} />
      </HomeCard>
      <HomeCard>
        <SkeletonBlock height={20} />
        <SkeletonBlock height={64} />
      </HomeCard>
      <HomeCard>
        <SkeletonBlock height={20} />
        <SkeletonBlock height={40} />
      </HomeCard>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 12,
  },
  block: {
    borderRadius: 6,
    opacity: 0.35,
  },
});
