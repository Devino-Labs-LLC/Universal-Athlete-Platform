import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Surface } from '@/src/core/components/Surface';
import {
  BODY_AREAS,
  BODY_SIDES,
  bodyAreaLabel,
  bodySideLabel,
} from '@/src/features/recovery/models/recoveryLabels';
import { normalizeDiscomfortSide } from '@/src/features/recovery/models/recoverySchemas';

export interface DiscomfortEntry {
  bodyArea: string;
  side: string;
  intensity: number;
  notes?: string;
}

interface DiscomfortEditorProps {
  value: DiscomfortEntry[];
  onChange: (entries: DiscomfortEntry[]) => void;
  testID?: string;
}

const DEFAULT_ENTRY: DiscomfortEntry = {
  bodyArea: 'LOWER_BACK',
  side: 'CENTER',
  intensity: 2,
};

export function DiscomfortEditor({ value, onChange, testID }: DiscomfortEditorProps) {
  const theme = useAppTheme();

  const addEntry = () => {
    if (value.length >= 20) {
      return;
    }
    onChange([...value, { ...DEFAULT_ENTRY }]);
  };

  const removeEntry = (index: number) => {
    onChange(value.filter((_, i) => i !== index));
  };

  const updateEntry = (index: number, patch: Partial<DiscomfortEntry>) => {
    const next = value.map((entry, i) => {
      if (i !== index) {
        return entry;
      }
      const merged = { ...entry, ...patch };
      if (patch.bodyArea === 'GENERAL_FULL_BODY' || merged.bodyArea === 'GENERAL_FULL_BODY') {
        merged.side = 'NOT_APPLICABLE';
      }
      merged.side = normalizeDiscomfortSide(merged.bodyArea, merged.side);
      return merged;
    });
    onChange(next);
  };

  return (
    <View style={styles.container} testID={testID}>
      <Text style={[styles.title, { color: theme.colors.text }]}>Discomfort (optional)</Text>
      {value.length === 0 ? (
        <Text style={[styles.hint, { color: theme.colors.textMuted }]}>
          Add body areas where you feel discomfort today.
        </Text>
      ) : null}

      {value.map((entry, index) => (
        <Surface key={`${entry.bodyArea}-${entry.side}-${index}`} elevated style={styles.entry}>
          <Text style={[styles.entryTitle, { color: theme.colors.text }]}>
            Area {index + 1}
          </Text>

          <Text style={[styles.label, { color: theme.colors.textMuted }]}>Body area</Text>
          <View style={styles.chipRow}>
            {BODY_AREAS.map((area) => {
              const selected = entry.bodyArea === area;
              return (
                <Pressable
                  key={area}
                  accessibilityRole="button"
                  accessibilityState={{ selected }}
                  onPress={() => updateEntry(index, { bodyArea: area })}
                  style={[
                    styles.chip,
                    {
                      borderColor: selected ? theme.colors.accentCyan : theme.colors.border,
                      backgroundColor: selected
                        ? theme.colors.accentCyanMuted
                        : theme.colors.surface,
                      minHeight: 44,
                    },
                  ]}>
                  <Text
                    style={{
                      color: selected ? theme.colors.accentCyan : theme.colors.text,
                      fontSize: 12,
                      fontWeight: selected ? '700' : '500',
                    }}>
                    {bodyAreaLabel(area)}
                  </Text>
                </Pressable>
              );
            })}
          </View>

          {entry.bodyArea !== 'GENERAL_FULL_BODY' ? (
            <>
              <Text style={[styles.label, { color: theme.colors.textMuted }]}>Side</Text>
              <View style={styles.chipRow}>
                {BODY_SIDES.filter((s) => s !== 'NOT_APPLICABLE').map((side) => {
                  const selected = entry.side === side;
                  return (
                    <Pressable
                      key={side}
                      accessibilityRole="button"
                      accessibilityState={{ selected }}
                      onPress={() => updateEntry(index, { side })}
                      style={[
                        styles.chip,
                        {
                          borderColor: selected ? theme.colors.accentCyan : theme.colors.border,
                          backgroundColor: selected
                            ? theme.colors.accentCyanMuted
                            : theme.colors.surface,
                          minHeight: 44,
                        },
                      ]}>
                      <Text
                        style={{
                          color: selected ? theme.colors.accentCyan : theme.colors.text,
                          fontSize: 12,
                          fontWeight: selected ? '700' : '500',
                        }}>
                        {bodySideLabel(side)}
                      </Text>
                    </Pressable>
                  );
                })}
              </View>
            </>
          ) : (
            <Text style={[styles.hint, { color: theme.colors.textMuted }]}>
              Side: {bodySideLabel('NOT_APPLICABLE')}
            </Text>
          )}

          <Text style={[styles.label, { color: theme.colors.textMuted }]}>Intensity (1–5)</Text>
          <View style={styles.intensityRow}>
            {[1, 2, 3, 4, 5].map((level) => {
              const selected = entry.intensity === level;
              return (
                <Pressable
                  key={level}
                  accessibilityRole="button"
                  accessibilityState={{ selected }}
                  onPress={() => updateEntry(index, { intensity: level })}
                  style={[
                    styles.intensityButton,
                    {
                      borderColor: selected ? theme.colors.primary : theme.colors.border,
                      backgroundColor: selected
                        ? theme.colors.primaryMuted
                        : theme.colors.surface,
                      minHeight: 44,
                    },
                  ]}>
                  <Text
                    style={{
                      color: selected ? theme.colors.primary : theme.colors.text,
                      fontWeight: '700',
                    }}>
                    {level}
                  </Text>
                </Pressable>
              );
            })}
          </View>

          <TextInput
            accessibilityLabel="Discomfort notes"
            placeholder="Notes (optional, max 250)"
            placeholderTextColor={theme.colors.textMuted}
            value={entry.notes ?? ''}
            onChangeText={(text) => updateEntry(index, { notes: text.slice(0, 250) })}
            style={[
              styles.notesInput,
              {
                borderColor: theme.colors.border,
                color: theme.colors.text,
                backgroundColor: theme.colors.surface,
              },
            ]}
          />

          <Button
            variant="ghost"
            label="Remove"
            onPress={() => removeEntry(index)}
            accessibilityLabel="Remove discomfort area"
          />
        </Surface>
      ))}

      <PrimaryButton
        label={value.length >= 20 ? 'Maximum areas reached' : 'Add discomfort area'}
        onPress={addEntry}
        disabled={value.length >= 20}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 8,
  },
  title: {
    fontSize: 15,
    fontWeight: '600',
  },
  hint: {
    fontSize: 13,
  },
  entry: {
    gap: 8,
  },
  entryTitle: {
    fontSize: 14,
    fontWeight: '600',
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  chip: {
    borderWidth: 1.5,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    justifyContent: 'center',
  },
  intensityRow: {
    flexDirection: 'row',
    gap: 6,
  },
  intensityButton: {
    flex: 1,
    borderWidth: 1.5,
    borderRadius: 10,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  notesInput: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    minHeight: 44,
  },
});
