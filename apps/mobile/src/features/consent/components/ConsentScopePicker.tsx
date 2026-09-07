import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import {
  allConsentScopes,
  consentScopeDescription,
  consentScopeLabel,
} from '@/src/features/consent/models/consentLabels';
import { ConsentScope } from '@/src/features/consent/models/consentSchemas';

interface ConsentScopePickerProps {
  selected: ConsentScope[];
  onChange: (next: ConsentScope[]) => void;
  testID?: string;
}

export function ConsentScopePicker({ selected, onChange, testID }: ConsentScopePickerProps) {
  const theme = useAppTheme();
  const scopes = allConsentScopes();

  const toggle = (scope: ConsentScope) => {
    if (selected.includes(scope)) {
      onChange(selected.filter((value) => value !== scope));
      return;
    }
    onChange([...selected, scope]);
  };

  return (
    <View testID={testID ?? 'consent-scope-picker'} style={styles.container}>
      <Text style={[styles.count, { color: theme.colors.textMuted }]}>
        {selected.length} selected · none are required by default
      </Text>
      <View style={styles.list}>
        {scopes.map((scope) => {
          const isSelected = selected.includes(scope);
          const label = consentScopeLabel(scope);
          return (
            <Pressable
              key={scope}
              accessibilityRole="checkbox"
              accessibilityLabel={label}
              accessibilityHint={consentScopeDescription(scope)}
              accessibilityState={{ checked: isSelected }}
              onPress={() => toggle(scope)}
              style={[
                styles.row,
                {
                  borderColor: isSelected ? theme.colors.accentCyan : theme.colors.border,
                  backgroundColor: isSelected
                    ? theme.colors.accentCyanMuted
                    : theme.colors.surface,
                  minHeight: 44,
                },
              ]}
              testID={`consent-scope-${scope}`}>
              <View style={styles.flex}>
                <Text style={{ color: theme.colors.text, fontWeight: '600' }}>{label}</Text>
                <Text style={{ color: theme.colors.textMuted, fontSize: 13, marginTop: 2 }}>
                  {consentScopeDescription(scope)}
                </Text>
              </View>
              <Text
                style={{
                  color: isSelected ? theme.colors.accentCyan : theme.colors.textMuted,
                  fontWeight: isSelected ? '700' : '500',
                }}>
                {isSelected ? 'Selected' : 'Off'}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 10,
  },
  count: {
    fontSize: 13,
  },
  list: {
    gap: 8,
  },
  row: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  flex: {
    flex: 1,
  },
});
