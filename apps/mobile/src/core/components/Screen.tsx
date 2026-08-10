import { PropsWithChildren } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface ScreenProps extends PropsWithChildren {
  title?: string;
  scroll?: boolean;
  style?: ViewStyle;
  refreshing?: boolean;
  onRefresh?: () => void;
  testID?: string;
}

export function Screen({
  title,
  scroll = false,
  style,
  refreshing,
  onRefresh,
  testID,
  children,
}: ScreenProps) {
  const theme = useAppTheme();
  const content = (
    <>
      {title ? (
        <Text style={[styles.title, { color: theme.colors.text }]}>{title}</Text>
      ) : null}
      {children}
    </>
  );

  const refreshControl =
    onRefresh != null ? (
      <RefreshControl
        refreshing={refreshing ?? false}
        onRefresh={onRefresh}
        tintColor={theme.colors.primary}
        colors={[theme.colors.primary]}
      />
    ) : undefined;

  return (
    <SafeAreaView
      style={[styles.safeArea, { backgroundColor: theme.colors.background }, style]}
      edges={['top', 'left', 'right']}>
      {scroll ? (
        <ScrollView
          testID={testID}
          contentContainerStyle={styles.content}
          refreshControl={refreshControl}>
          {content}
        </ScrollView>
      ) : (
        <View style={styles.content}>{content}</View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    flexGrow: 1,
    padding: 16,
    gap: 12,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
  },
});
