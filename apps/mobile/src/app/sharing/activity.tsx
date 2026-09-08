import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { fetchAthleteTransparency } from '@/src/features/consent/api/consentsApi';
import { useQuery } from '@tanstack/react-query';

export default function TeamActivityScreen() {
  const theme = useAppTheme();
  const { apiClient, account } = useAuthSession();
  const [page, setPage] = useState(0);
  const activityQuery = useQuery({
    queryKey: ['athlete', account?.accountId ?? '', 'transparency', page],
    queryFn: () => fetchAthleteTransparency(apiClient, page),
    enabled: Boolean(account?.accountId),
  });

  return (
    <Screen title="Team activity" scroll>
      {activityQuery.isLoading ? <Text style={{ color: theme.colors.textMuted }}>Loading activity…</Text> : null}
      {activityQuery.isError ? (
        <Text style={{ color: theme.colors.text }}>Unable to load activity.</Text>
      ) : null}
      {activityQuery.data?.events.length === 0 ? (
        <Text style={{ color: theme.colors.textMuted }}>
          Joining a team, sharing, or responding to a coach assignment will appear here.
        </Text>
      ) : null}
      {activityQuery.data?.events.map((event) => (
        <View key={`${event.type}-${event.occurredAt}-${event.teamName ?? ''}`} style={styles.item}>
          <Text style={{ color: theme.colors.text }}>{event.description}</Text>
          <Text style={{ color: theme.colors.textMuted }}>
            {[event.organizationName, event.teamName].filter(Boolean).join(' · ')}
          </Text>
        </View>
      ))}
      {activityQuery.data?.hasMore ? (
        <Button label="Older activity" onPress={() => setPage((current) => current + 1)} />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  item: { marginBottom: 16 },
});
