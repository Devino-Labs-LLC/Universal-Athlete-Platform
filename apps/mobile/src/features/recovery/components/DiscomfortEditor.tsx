import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
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
        <View
          key={`${entry.bodyArea}-${entry.side}-${index}`}
          style={[styles.entry, { borderColor: theme.colors.border }]}>
          <Text style={[styles.entryTitle, { color: theme.colors.text }]}>
            Area {index + 1}
          </Text>

          <Text style={[styles.label, { color: theme.colors.textMuted }]}>Body area</Text>
          <View style={styles.chipRow}>
            {BODY_AREAS.slice(0, 8).map((area) => (
              <Pressable
                key={area}
                accessibilityRole="button"
                onPress={() => updateEntry(index, { bodyArea: area })}
                style={[
                  styles.chip,
                  {
                    borderColor: entry.bodyArea === area ? theme.colors.primary : theme.colors.border,
                    backgroundColor:
                      entry.bodyArea === area ? theme.colors.primary : theme.colors.surface,
                  },
                ]}>
                <Text
                  style={{
                    color: entry.bodyArea === area ? theme.colors.primaryText : theme.colors.text,
                    fontSize: 11,
                  }}>
                  {bodyAreaLabel(area)}
                </Text>
              </Pressable>
            ))}
          </View>
          <View style={styles.chipRow}>
            {BODY_AREAS.slice(8).map((area) => (
              <Pressable
                key={area}
                accessibilityRole="button"
                onPress={() => updateEntry(index, { bodyArea: area })}
                style={[
                  styles.chip,
                  {
                    borderColor: entry.bodyArea === area ? theme.colors.primary : theme.colors.border,
                    backgroundColor:
                      entry.bodyArea === area ? theme.colors.primary : theme.colors.surface,
                  },
                ]}>
                <Text
                  style={{
                    color: entry.bodyArea === area ? theme.colors.primaryText : theme.colors.text,
                    fontSize: 11,
                  }}>
                  {bodyAreaLabel(area)}
                </Text>
              </Pressable>
            ))}
          </View>

          {entry.bodyArea !== 'GENERAL_FULL_BODY' ? (
            <>
              <Text style={[styles.label, { color: theme.colors.textMuted }]}>Side</Text>
              <View style={styles.chipRow}>
                {BODY_SIDES.filter((s) => s !== 'NOT_APPLICABLE').map((side) => (
                  <Pressable
                    key={side}
                    accessibilityRole="button"
                    onPress={() => updateEntry(index, { side })}
                    style={[
                      styles.chip,
                      {
                        borderColor: entry.side === side ? theme.colors.primary : theme.colors.border,
                        backgroundColor:
                          entry.side === side ? theme.colors.primary : theme.colors.surface,
                      },
                    ]}>
                    <Text
                      style={{
                        color: entry.side === side ? theme.colors.primaryText : theme.colors.text,
                        fontSize: 11,
                      }}>
                      {bodySideLabel(side)}
                    </Text>
                  </Pressable>
                ))}
              </View>
            </>
          ) : (
            <Text style={[styles.hint, { color: theme.colors.textMuted }]}>
              Side: {bodySideLabel('NOT_APPLICABLE')}
            </Text>
          )}

          <Text style={[styles.label, { color: theme.colors.textMuted }]}>Intensity (1–5)</Text>
          <View style={styles.intensityRow}>
            {[1, 2, 3, 4, 5].map((level) => (
              <Pressable
                key={level}
                accessibilityRole="button"
                onPress={() => updateEntry(index, { intensity: level })}
                style={[
                  styles.intensityButton,
                  {
                    borderColor: entry.intensity === level ? theme.colors.primary : theme.colors.border,
                    backgroundColor:
                      entry.intensity === level ? theme.colors.primary : theme.colors.surface,
                  },
                ]}>
                <Text
                  style={{
                    color: entry.intensity === level ? theme.colors.primaryText : theme.colors.text,
                    fontWeight: '700',
                  }}>
                  {level}
                </Text>
              </Pressable>
            ))}
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

          <Pressable accessibilityRole="button" onPress={() => removeEntry(index)}>
            <Text style={[styles.remove, { color: theme.colors.danger }]}>Remove</Text>
          </Pressable>
        </View>
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
    borderWidth: 1,
    borderRadius: 10,
    padding: 12,
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
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  intensityRow: {
    flexDirection: 'row',
    gap: 6,
  },
  intensityButton: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 8,
    paddingVertical: 8,
    alignItems: 'center',
  },
  notesInput: {
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    fontSize: 14,
  },
  remove: {
    fontSize: 14,
    fontWeight: '600',
  },
});
