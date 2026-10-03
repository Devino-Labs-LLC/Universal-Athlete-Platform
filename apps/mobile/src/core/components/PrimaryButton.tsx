import { PropsWithChildren } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  ViewStyle,
} from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

export type ButtonVariant = 'primary' | 'secondary' | 'destructive' | 'ghost';

interface ButtonProps {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  loading?: boolean;
  variant?: ButtonVariant;
  testID?: string;
  style?: ViewStyle;
  accessibilityLabel?: string;
}

export function Button({
  label,
  onPress,
  disabled = false,
  loading = false,
  variant = 'primary',
  testID,
  style,
  accessibilityLabel,
}: ButtonProps) {
  const theme = useAppTheme();
  const isDisabled = disabled || loading;

  const palette = (() => {
    switch (variant) {
      case 'secondary':
        return {
          background: theme.colors.actionSecondary,
          border: theme.colors.borderStrong,
          label: theme.colors.textPrimary,
          pressedBackground: theme.colors.surfaceMuted,
        };
      case 'destructive':
        return {
          background: theme.colors.danger,
          border: theme.colors.danger,
          label: theme.colors.dangerText,
          pressedBackground: theme.colors.danger,
        };
      case 'ghost':
        return {
          background: 'transparent',
          border: 'transparent',
          label: theme.colors.accentCyan,
          pressedBackground: theme.colors.accentCyanMuted,
        };
      case 'primary':
      default:
        return {
          background: theme.colors.actionPrimary,
          border: theme.colors.actionPrimary,
          label: theme.colors.textInverse,
          pressedBackground: theme.colors.actionPrimaryPressed,
        };
    }
  })();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      testID={testID}
      disabled={isDisabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        {
          backgroundColor: pressed && !isDisabled ? palette.pressedBackground : palette.background,
          borderColor: palette.border,
          borderRadius: theme.radius.md,
          opacity: isDisabled ? 0.5 : 1,
        },
        style,
      ]}>
      {loading ? (
        <ActivityIndicator color={palette.label} />
      ) : (
        <Text style={[styles.label, { color: palette.label }]}>{label}</Text>
      )}
    </Pressable>
  );
}

/** Lime primary CTA — preferred for meaningful actions. */
export function PrimaryButton(props: Omit<ButtonProps, 'variant'>) {
  return <Button {...props} variant="primary" />;
}

const styles = StyleSheet.create({
  button: {
    minHeight: 44,
    borderWidth: 1,
    paddingHorizontal: 16,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
  },
});

/** Optional keyboard wrapper for form screens. */
export function KeyboardScreen({ children, style }: PropsWithChildren<{ style?: ViewStyle }>) {
  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[{ flex: 1 }, style]}>
      {children}
    </KeyboardAvoidingView>
  );
}
