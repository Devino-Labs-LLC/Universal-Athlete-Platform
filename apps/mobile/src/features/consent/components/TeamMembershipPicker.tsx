import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { MyAthleteTeamMembership } from '@/src/features/consent/models/consentSchemas';

interface TeamMembershipPickerProps {
  memberships: MyAthleteTeamMembership[];
  selectedTeamId: string | null;
  onSelect: (teamId: string) => void;
  /** Team IDs that already have an ACTIVE grant and should be disabled. */
  disabledTeamIds?: Set<string>;
  testID?: string;
}

export function TeamMembershipPicker({
  memberships,
  selectedTeamId,
  onSelect,
  disabledTeamIds,
  testID,
}: TeamMembershipPickerProps) {
  const theme = useAppTheme();

  if (memberships.length === 0) {
    return (
      <Text
        testID={`${testID ?? 'team-membership-picker'}-empty`}
        style={{ color: theme.colors.textMuted }}>
        Join a team first to share data with coaches.
      </Text>
    );
  }

  return (
    <View testID={testID ?? 'team-membership-picker'} style={styles.list}>
      {memberships.map((membership) => {
        const isSelected = selectedTeamId === membership.teamId;
        const isDisabled = disabledTeamIds?.has(membership.teamId) ?? false;
        return (
          <Pressable
            key={membership.membershipId}
            accessibilityRole="radio"
            accessibilityLabel={`${membership.teamName}, ${membership.organizationName}`}
            accessibilityState={{ selected: isSelected, disabled: isDisabled }}
            disabled={isDisabled}
            onPress={() => onSelect(membership.teamId)}
            style={[
              styles.row,
              {
                borderColor: isSelected ? theme.colors.accentCyan : theme.colors.border,
                backgroundColor: isSelected
                  ? theme.colors.accentCyanMuted
                  : theme.colors.surface,
                opacity: isDisabled ? 0.5 : 1,
                minHeight: 44,
              },
            ]}
            testID={`team-option-${membership.teamId}`}>
            <View style={styles.flex}>
              <Text style={{ color: theme.colors.text, fontWeight: '600' }}>
                {membership.teamName}
              </Text>
              <Text style={{ color: theme.colors.textMuted, fontSize: 13 }}>
                {membership.organizationName}
                {isDisabled ? ' · already sharing' : ''}
              </Text>
            </View>
            <Text
              style={{
                color: isSelected ? theme.colors.accentCyan : theme.colors.textMuted,
                fontWeight: isSelected ? '700' : '500',
              }}>
              {isSelected ? 'Selected' : isDisabled ? 'Active' : 'Select'}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
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
