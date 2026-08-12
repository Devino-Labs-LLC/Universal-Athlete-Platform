import { PropsWithChildren, ReactNode } from 'react';
import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
  ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface ScreenProps extends PropsWithChildren {
  title?: string;
  description?: string;
  scroll?: boolean;
  style?: ViewStyle;
  contentStyle?: ViewStyle;
  refreshing?: boolean;
  onRefresh?: () => void;
  testID?: string;
  /** Include bottom safe area (use when no tab bar is present). */
  includeBottomInset?: boolean;
  headerRight?: ReactNode;
}

export function Screen({
  title,
  description,
  scroll = false,
  style,
  contentStyle,
  refreshing,
  onRefresh,
  testID,
  includeBottomInset = false,
  headerRight,
  children,
}: ScreenProps) {
  const theme = useAppTheme();
  const edges = includeBottomInset
    ? (['top', 'left', 'right', 'bottom'] as const)
    : (['top', 'left', 'right'] as const);

  const content = (
    <>
      {title || headerRight ? (
        <View style={styles.headerRow}>
          <View style={styles.headerText}>
            {title ? (
              <Text
                accessibilityRole="header"
                style={[
                  styles.title,
                  {
                    color: theme.colors.text,
                    fontSize: theme.typography.pageTitle,
                  },
                ]}>
                {title}
              </Text>
            ) : null}
            {description ? (
              <Text style={[styles.description, { color: theme.colors.textMuted }]}>
                {description}
              </Text>
            ) : null}
          </View>
          {headerRight}
        </View>
      ) : null}
      {children}
    </>
  );

  const refreshControl =
    onRefresh != null ? (
      <RefreshControl
        refreshing={refreshing ?? false}
        onRefresh={onRefresh}
        tintColor={theme.colors.accentCyan}
        colors={[theme.colors.accentCyan]}
      />
    ) : undefined;

  return (
    <SafeAreaView
      style={[styles.safeArea, { backgroundColor: theme.colors.background }, style]}
      edges={[...edges]}>
      {scroll ? (
        <ScrollView
          testID={testID}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={[styles.content, contentStyle]}
          refreshControl={refreshControl}>
          {content}
        </ScrollView>
      ) : (
        <View testID={testID} style={[styles.content, contentStyle]}>
          {content}
        </View>
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
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 24,
    gap: 16,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  headerText: {
    flex: 1,
    gap: 4,
    minWidth: 0,
  },
  title: {
    fontWeight: '700',
  },
  description: {
    fontSize: 15,
    lineHeight: 21,
  },
});
