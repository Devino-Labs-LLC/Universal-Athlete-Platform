import { PropsWithChildren } from 'react';
import { ScrollView, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface ScreenProps extends PropsWithChildren {
  title?: string;
  scroll?: boolean;
  style?: ViewStyle;
}

export function Screen({ title, scroll = false, style, children }: ScreenProps) {
  const theme = useAppTheme();
  const content = (
    <>
      {title ? (
        <Text style={[styles.title, { color: theme.colors.text }]}>{title}</Text>
      ) : null}
      {children}
    </>
  );

  return (
    <SafeAreaView
      style={[styles.safeArea, { backgroundColor: theme.colors.background }, style]}
      edges={['top', 'left', 'right']}>
      {scroll ? (
        <ScrollView contentContainerStyle={styles.content}>{content}</ScrollView>
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
