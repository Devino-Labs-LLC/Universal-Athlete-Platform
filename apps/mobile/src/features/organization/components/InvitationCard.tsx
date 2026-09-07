import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  formatInvitationExpiry,
  invitationRoleLabel,
  invitationScopeSubtitle,
  invitationScopeTitle,
} from '@/src/features/organization/models/invitationLabels';
import { MyInvitation } from '@/src/features/organization/models/invitationSchemas';

interface InvitationCardProps {
  invitation: MyInvitation;
  busy: boolean;
  onAccept: () => void;
  onDecline: () => void;
  testID?: string;
}

export function InvitationCard({
  invitation,
  busy,
  onAccept,
  onDecline,
  testID,
}: InvitationCardProps) {
  const theme = useAppTheme();
  const title = invitationScopeTitle(invitation);
  const subtitle = invitationScopeSubtitle(invitation);
  const scopeLabel = invitation.teamName ? 'Team invitation' : 'Organization invitation';

  return (
    <HomeCard
      eyebrow={scopeLabel}
      title={title}
      subtitle={subtitle}
      testID={testID}>
      <CompactInfoRow label="Role" value={invitationRoleLabel(invitation.role)} />
      <CompactInfoRow label="Expires" value={formatInvitationExpiry(invitation.expiresAt)} />
      <View style={styles.actions}>
        <Button
          variant="secondary"
          label="Decline"
          disabled={busy}
          onPress={onDecline}
          testID={`${testID ?? 'invitation'}-decline`}
          accessibilityLabel={`Decline invitation to ${title}`}
          style={styles.flex}
        />
        <Button
          label="Accept"
          disabled={busy}
          loading={busy}
          onPress={onAccept}
          testID={`${testID ?? 'invitation'}-accept`}
          accessibilityLabel={`Accept invitation to ${title}`}
          style={styles.flex}
        />
      </View>
      <Text style={[styles.hint, { color: theme.colors.textMuted }]}>
        Accepting joins you with the role shown above. You can leave later from membership
        settings when available.
      </Text>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: 'row',
    gap: 8,
  },
  flex: {
    flex: 1,
  },
  hint: {
    fontSize: 12,
    lineHeight: 16,
  },
});
