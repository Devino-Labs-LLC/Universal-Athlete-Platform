import Constants from 'expo-constants';
import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { loadAppConfig } from '@/src/app/config/env';
import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { Button } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { EXPECTED_CLIENT_CONTRACT_VERSION } from '@/src/features/training/api';
import { useTrainingEnvironments } from '@/src/features/environments/hooks/useTrainingEnvironments';
import { trainingEnvironmentTypeLabel } from '@/src/features/environments/models/environmentLabels';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import {
  useDeleteAthleteGoalMutation,
} from '@/src/features/profile/hooks/useAthleteGoals';
import {
  useDeleteAthleteSportMutation,
} from '@/src/features/profile/hooks/useAthleteSports';

export default function ProfileScreen() {
  const theme = useAppTheme();
  const appConfig = loadAppConfig();
  const appVersion = Constants.expoConfig?.version;
  const { account, logout, logoutAll } = useAuthSession();
  const { snapshot, refresh } = useAthleteOnboarding();
  const { bootstrap } = useBootstrap();
  const deleteSportMutation = useDeleteAthleteSportMutation();
  const deleteGoalMutation = useDeleteAthleteGoalMutation();
  const environmentsQuery = useTrainingEnvironments({ activeOnly: true, size: 50 });
  const [confirmAction, setConfirmAction] = useState<'logout' | 'logoutAll' | null>(null);
  const [busy, setBusy] = useState(false);

  const defaultEnvironment = environmentsQuery.data?.environments.find(
    (environment) => environment.defaultEnvironment,
  );

  const handleLogout = async () => {
    setBusy(true);
    try {
      await logout();
      router.replace('/(auth)/login');
    } finally {
      setBusy(false);
      setConfirmAction(null);
    }
  };

  const handleLogoutAll = async () => {
    setBusy(true);
    try {
      await logoutAll();
      router.replace('/(auth)/login');
    } finally {
      setBusy(false);
      setConfirmAction(null);
    }
  };

  const displayName = snapshot.profile
    ? `${snapshot.profile.firstName} ${snapshot.profile.lastName}`
    : account?.email ?? 'Athlete';

  return (
    <Screen title="Profile" scroll>
      <HomeCard eyebrow="Identity" title={displayName}>
        <CompactInfoRow label="Email" value={account?.email ?? 'Unknown'} />
        <CompactInfoRow label="Status" value={account?.status ?? 'Unknown'} />
        {snapshot.profile ? (
          <>
            <CompactInfoRow label="Date of birth" value={snapshot.profile.dateOfBirth} />
            <CompactInfoRow label="Sex" value={formatEnumLabel(snapshot.profile.sex)} />
            <CompactInfoRow
              label="Height / weight"
              value={`${snapshot.profile.heightCm} cm · ${snapshot.profile.weightKg} kg`}
            />
            <Button
              variant="secondary"
              label="Edit profile"
              onPress={() => router.push('/(onboarding)/profile')}
            />
          </>
        ) : (
          <Text style={{ color: theme.colors.textMuted }}>No athlete profile yet.</Text>
        )}
      </HomeCard>

      <HomeCard eyebrow="Sports" title="Sports summary">
        {snapshot.sports.length === 0 ? (
          <Text style={{ color: theme.colors.textMuted }}>No sports added.</Text>
        ) : (
          snapshot.sports.map((sport) => (
            <View key={sport.id} style={styles.row}>
              <View style={styles.flex}>
                <Text style={{ color: theme.colors.text, fontWeight: '600' }}>
                  {sport.customSportName ?? formatEnumLabel(sport.sportType)}
                </Text>
                <Text style={{ color: theme.colors.textMuted, fontSize: 13 }}>
                  {formatEnumLabel(sport.participationLevel)} · {sport.yearsExperience} yrs
                </Text>
              </View>
              <Button
                variant="ghost"
                label="Remove"
                onPress={() => {
                  void deleteSportMutation.mutateAsync(sport.id).then(() => refresh());
                }}
              />
            </View>
          ))
        )}
        <Button
          variant="secondary"
          label="Add sport"
          onPress={() => router.push('/(onboarding)/sports')}
        />
      </HomeCard>

      <HomeCard eyebrow="Goals" title="Goals summary">
        {snapshot.goals.length === 0 ? (
          <Text style={{ color: theme.colors.textMuted }}>No goals added.</Text>
        ) : (
          snapshot.goals.map((goal) => (
            <View key={goal.id} style={styles.row}>
              <View style={styles.flex}>
                <Text style={{ color: theme.colors.text, fontWeight: '600' }}>{goal.title}</Text>
                <Text style={{ color: theme.colors.textMuted, fontSize: 13 }}>
                  {formatEnumLabel(goal.goalType)}
                  {goal.priority ? ` · ${formatEnumLabel(goal.priority)}` : ''}
                </Text>
              </View>
              <Button
                variant="ghost"
                label="Remove"
                onPress={() => {
                  void deleteGoalMutation.mutateAsync(goal.id).then(() => refresh());
                }}
              />
            </View>
          ))
        )}
        <Button
          variant="secondary"
          label="Add goal"
          onPress={() => router.push('/(onboarding)/goals')}
        />
      </HomeCard>

      <HomeCard eyebrow="Training" title="Environments">
        <Button
          variant="secondary"
          label="Training Environments"
          testID="profile-training-environments-link"
          onPress={() => router.push('/(tabs)/profile/environments')}
        />
        {defaultEnvironment ? (
          <CompactInfoRow
            label="Default environment"
            value={`${defaultEnvironment.name} · ${trainingEnvironmentTypeLabel(defaultEnvironment.type)}`}
          />
        ) : (
          <Text style={{ color: theme.colors.textMuted }}>No default environment set.</Text>
        )}
      </HomeCard>

      <HomeCard eyebrow="Client" title="App info">
        <CompactInfoRow label="Environment" value={appConfig.environment} />
        <CompactInfoRow
          label="Client contract"
          value={bootstrap?.clientContractVersion ?? EXPECTED_CLIENT_CONTRACT_VERSION}
        />
        {appVersion ? <CompactInfoRow label="App version" value={appVersion} /> : null}
      </HomeCard>

      <View style={styles.accountActions}>
        <Button
          variant="secondary"
          label="Sign out"
          disabled={busy}
          onPress={() => setConfirmAction('logout')}
        />
        <Button
          variant="destructive"
          label="Sign out everywhere"
          disabled={busy}
          onPress={() => setConfirmAction('logoutAll')}
        />
      </View>

      <ConfirmationDialog
        visible={confirmAction === 'logout'}
        title="Sign out?"
        message="You will need to sign in again to access your account."
        confirmLabel="Sign out"
        onCancel={() => setConfirmAction(null)}
        onConfirm={() => void handleLogout()}
      />
      <ConfirmationDialog
        visible={confirmAction === 'logoutAll'}
        title="Sign out everywhere?"
        message="This ends all active sessions for your account on every device."
        confirmLabel="Sign out everywhere"
        destructive
        onCancel={() => setConfirmAction(null)}
        onConfirm={() => void handleLogoutAll()}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  flex: {
    flex: 1,
  },
  accountActions: {
    gap: 8,
  },
});
