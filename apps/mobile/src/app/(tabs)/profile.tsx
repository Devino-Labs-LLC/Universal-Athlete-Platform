import { Link, router } from 'expo-router';
import { useState } from 'react';
import { PropsWithChildren } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { loadAppConfig } from '@/src/app/config/env';
import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { EXPECTED_CLIENT_CONTRACT_VERSION } from '@/src/features/training/api';
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
  const { account, logout, logoutAll } = useAuthSession();
  const { snapshot, refresh } = useAthleteOnboarding();
  const { bootstrap } = useBootstrap();
  const deleteSportMutation = useDeleteAthleteSportMutation();
  const deleteGoalMutation = useDeleteAthleteGoalMutation();
  const [confirmAction, setConfirmAction] = useState<'logout' | 'logoutAll' | null>(null);
  const [busy, setBusy] = useState(false);

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

  return (
    <Screen title="Profile" scroll>
      <Section title="Account">
        <LabelValue label="Email" value={account?.email ?? 'Unknown'} />
        <LabelValue label="Status" value={account?.status ?? 'Unknown'} />
      </Section>

      <Section title="Athlete">
        {snapshot.profile ? (
          <>
            <LabelValue
              label="Name"
              value={`${snapshot.profile.firstName} ${snapshot.profile.lastName}`}
            />
            <LabelValue label="Date of birth" value={snapshot.profile.dateOfBirth} />
            <LabelValue label="Sex" value={formatEnumLabel(snapshot.profile.sex)} />
            <LabelValue
              label="Height / weight"
              value={`${snapshot.profile.heightCm} cm · ${snapshot.profile.weightKg} kg`}
            />
            <Link href="/(onboarding)/profile" asChild>
              <Pressable accessibilityRole="button">
                <Text style={{ color: theme.colors.primary }}>Edit profile</Text>
              </Pressable>
            </Link>
          </>
        ) : (
          <Text style={{ color: theme.colors.textMuted }}>No athlete profile yet.</Text>
        )}
      </Section>

      <Section title="Sports">
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
              <Pressable
                accessibilityRole="button"
                onPress={() => {
                  void deleteSportMutation.mutateAsync(sport.id).then(() => refresh());
                }}>
                <Text style={{ color: theme.colors.danger }}>Remove</Text>
              </Pressable>
            </View>
          ))
        )}
        <Link href="/(onboarding)/sports" asChild>
          <Pressable accessibilityRole="button">
            <Text style={{ color: theme.colors.primary }}>Add sport</Text>
          </Pressable>
        </Link>
      </Section>

      <Section title="Goals">
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
              <Pressable
                accessibilityRole="button"
                onPress={() => {
                  void deleteGoalMutation.mutateAsync(goal.id).then(() => refresh());
                }}>
                <Text style={{ color: theme.colors.danger }}>Remove</Text>
              </Pressable>
            </View>
          ))
        )}
        <Link href="/(onboarding)/goals" asChild>
          <Pressable accessibilityRole="button">
            <Text style={{ color: theme.colors.primary }}>Add goal</Text>
          </Pressable>
        </Link>
      </Section>

      <Section title="Client">
        <LabelValue label="Environment" value={appConfig.environment} />
        <LabelValue
          label="Client contract"
          value={bootstrap?.clientContractVersion ?? EXPECTED_CLIENT_CONTRACT_VERSION}
        />
      </Section>

      <PrimaryButton
        label="Sign out"
        disabled={busy}
        onPress={() => setConfirmAction('logout')}
      />
      <Pressable accessibilityRole="button" onPress={() => setConfirmAction('logoutAll')}>
        <Text style={[styles.logoutAll, { color: theme.colors.danger }]}>Sign out everywhere</Text>
      </Pressable>

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

function Section({ title, children }: PropsWithChildren<{ title: string }>) {
  const theme = useAppTheme();
  return (
    <View style={styles.section}>
      <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>{title}</Text>
      {children}
    </View>
  );
}

function LabelValue({ label, value }: { label: string; value: string }) {
  const theme = useAppTheme();
  return (
    <View style={styles.labelValue}>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>{label}</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  section: {
    gap: 8,
    marginBottom: 8,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  labelValue: {
    gap: 2,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  value: {
    fontSize: 16,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  flex: {
    flex: 1,
  },
  logoutAll: {
    textAlign: 'center',
    fontSize: 15,
    fontWeight: '600',
    paddingVertical: 8,
  },
});
