import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { CompactInfoRow } from '@/src/core/components/Surface';
import {
  consentStatusLabel,
  formatConsentScopesSummary,
  resolveTeamLabel,
} from '@/src/features/consent/models/consentLabels';
import {
  ConsentGrant,
  MyAthleteTeamMembership,
} from '@/src/features/consent/models/consentSchemas';
import { HomeCard } from '@/src/features/home/components/HomeCard';

interface ConsentGrantCardProps {
  grant: ConsentGrant;
  memberships: MyAthleteTeamMembership[];
  busy: boolean;
  onRevoke?: () => void;
  onReGrant?: () => void;
  testID?: string;
}

export function ConsentGrantCard({
  grant,
  memberships,
  busy,
  onRevoke,
  onReGrant,
  testID,
}: ConsentGrantCardProps) {
  const theme = useAppTheme();
  const { title, subtitle } = resolveTeamLabel(grant, memberships);
  const isActive = grant.status === 'ACTIVE';
  const canReGrant =
    !isActive &&
    memberships.some((membership) => membership.teamId === grant.teamId) &&
    onReGrant != null;

  return (
    <HomeCard
      eyebrow={consentStatusLabel(grant.status)}
      title={title}
      subtitle={subtitle}
      testID={testID}>
      <CompactInfoRow label="Scopes" value={formatConsentScopesSummary(grant.scopes)} />
      <Text style={[styles.scopes, { color: theme.colors.textMuted }]}>
        {grant.scopes.map((scope) => scope.replace(/_/g, ' ').toLowerCase()).join(' · ')}
      </Text>
      <View style={styles.actions}>
        {isActive && onRevoke ? (
          <Button
            variant="destructive"
            label="Stop sharing"
            disabled={busy}
            loading={busy}
            onPress={onRevoke}
            testID={`${testID ?? 'consent'}-revoke`}
            accessibilityLabel={`Stop sharing with ${title}`}
            style={styles.flex}
          />
        ) : null}
        {canReGrant ? (
          <Button
            label="Share again"
            disabled={busy}
            onPress={onReGrant}
            testID={`${testID ?? 'consent'}-regrant`}
            accessibilityLabel={`Share again with ${title}`}
            style={styles.flex}
          />
        ) : null}
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  scopes: {
    fontSize: 12,
    lineHeight: 16,
    textTransform: 'capitalize',
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
  },
  flex: {
    flex: 1,
  },
});
