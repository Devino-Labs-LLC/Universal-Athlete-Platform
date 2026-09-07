import { Href, router, useLocalSearchParams } from 'expo-router';
import { useMemo, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Button } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { ConsentScopePicker } from '@/src/features/consent/components/ConsentScopePicker';
import { TeamMembershipPicker } from '@/src/features/consent/components/TeamMembershipPicker';
import { useConsentMutations } from '@/src/features/consent/hooks/useConsentMutations';
import { useMyAthleteTeamMemberships } from '@/src/features/consent/hooks/useMyAthleteTeamMemberships';
import { useMyConsents } from '@/src/features/consent/hooks/useMyConsents';
import { ConsentScope } from '@/src/features/consent/models/consentSchemas';
import { consentErrorMessage } from '@/src/features/consent/utils/consentErrors';
import { HomeCard } from '@/src/features/home/components/HomeCard';

function firstParam(value: string | string[] | undefined): string | null {
  if (Array.isArray(value)) {
    return value[0] ?? null;
  }
  return value ?? null;
}

export function GrantSharingScreen() {
  const theme = useAppTheme();
  const params = useLocalSearchParams<{ teamId?: string | string[] }>();
  const initialTeamId = firstParam(params.teamId);

  const membershipsQuery = useMyAthleteTeamMemberships();
  const consentsQuery = useMyConsents();
  const { createMutation } = useConsentMutations();

  const memberships = membershipsQuery.data ?? [];

  const [selectedTeamId, setSelectedTeamId] = useState<string | null>(initialTeamId);
  const [selectedScopes, setSelectedScopes] = useState<ConsentScope[]>([]);

  const teamsWithActiveGrant = useMemo(() => {
    const ids = new Set<string>();
    for (const grant of consentsQuery.data ?? []) {
      if (grant.status === 'ACTIVE') {
        ids.add(grant.teamId);
      }
    }
    return ids;
  }, [consentsQuery.data]);

  const canSubmit =
    selectedTeamId != null &&
    selectedScopes.length > 0 &&
    !teamsWithActiveGrant.has(selectedTeamId) &&
    !createMutation.isPending;

  const handleSubmit = () => {
    if (!selectedTeamId || selectedScopes.length === 0) {
      return;
    }
    createMutation.mutate(
      { teamId: selectedTeamId, scopes: selectedScopes },
      {
        onSuccess: () => {
          Alert.alert('Sharing started', 'Your coaches can now see the scopes you selected.', [
            {
              text: 'OK',
              onPress: () => router.replace('/sharing' as Href),
            },
          ]);
        },
        onError: (error) => {
          Alert.alert('Could not start sharing', consentErrorMessage(error));
        },
      },
    );
  };

  if (
    (membershipsQuery.isLoading && !membershipsQuery.data) ||
    (consentsQuery.isLoading && !consentsQuery.data)
  ) {
    return <LoadingView message="Loading teams…" />;
  }

  if (membershipsQuery.isError && !membershipsQuery.data) {
    return (
      <ErrorView
        message={consentErrorMessage(membershipsQuery.error)}
        onRetry={() => {
          void membershipsQuery.refetch();
          void consentsQuery.refetch();
        }}
        testID="grant-sharing-error"
      />
    );
  }

  return (
    <Screen
      scroll
      title="Share with a team"
      description="Choose one team and the scopes you want to share. Nothing is selected by default."
      testID="grant-sharing-screen"
      includeBottomInset>
      <HomeCard eyebrow="Team" title="Who can see this?">
        <TeamMembershipPicker
          memberships={memberships}
          selectedTeamId={selectedTeamId}
          disabledTeamIds={teamsWithActiveGrant}
          onSelect={setSelectedTeamId}
        />
      </HomeCard>

      <HomeCard eyebrow="Scopes" title="What will they see?">
        <ConsentScopePicker selected={selectedScopes} onChange={setSelectedScopes} />
      </HomeCard>

      <View style={styles.footer}>
        <Text style={{ color: theme.colors.textMuted, fontSize: 13 }}>
          Granting is confirmed by the server. You can stop sharing anytime from the Sharing
          list.
        </Text>
        <Button
          label="Confirm sharing"
          disabled={!canSubmit}
          loading={createMutation.isPending}
          onPress={handleSubmit}
          testID="grant-sharing-submit"
          accessibilityLabel="Confirm sharing"
        />
        <Button
          variant="secondary"
          label="Cancel"
          disabled={createMutation.isPending}
          onPress={() => router.back()}
          testID="grant-sharing-cancel"
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  footer: {
    gap: 10,
    marginTop: 8,
  },
});
