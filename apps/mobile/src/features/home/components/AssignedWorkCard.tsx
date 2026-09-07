import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';

type Assignment = {
  id: string;
  title: string;
  description: string | null;
  scheduledDate: string;
  status: 'ASSIGNED' | 'DECLINED' | 'UNABLE';
  provenance: 'COACH_ASSIGNMENT';
};

export function AssignedWorkCard() {
  const theme = useAppTheme();
  const { apiClient, account, status } = useAuthSession();
  const queryClient = useQueryClient();
  const accountId = status === 'AUTHENTICATED' ? account?.accountId : null;

  const query = useQuery({
    queryKey: ['athlete-assignments', accountId ?? ''],
    enabled: Boolean(accountId),
    queryFn: async () => {
      const response = await apiClient.axios.get<Assignment[]>(
        '/api/v1/athletes/me/training/assignments',
      );
      return response.data.filter((item) => item.provenance === 'COACH_ASSIGNMENT');
    },
  });

  const respond = useMutation({
    mutationFn: async ({
      assignmentId,
      action,
    }: {
      assignmentId: string;
      action: 'decline' | 'unable';
    }) => {
      await apiClient.axios.post(
        `/api/v1/athletes/me/training/assignments/${assignmentId}/${action}`,
        { note: action === 'decline' ? 'Declined' : 'Unable to perform' },
      );
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['athlete-assignments', accountId ?? ''] });
    },
  });

  const open = (query.data ?? []).filter((item) => item.status === 'ASSIGNED');
  if (!accountId || open.length === 0) {
    return null;
  }

  return (
    <HomeCard testID="coach-assignment-card" eyebrow="Assigned by coach" title="Coach assignment">
      <Text style={{ color: theme.colors.textMuted }}>
        This is coach-assigned work, not Athlete Readiness guidance.
      </Text>
      {open.map((assignment) => (
        <View key={assignment.id}>
          <Text style={{ color: theme.colors.text }}>{assignment.title}</Text>
          <Pressable
            accessibilityRole="button"
            disabled={respond.isPending}
            onPress={() => respond.mutate({ assignmentId: assignment.id, action: 'decline' })}
            style={styles.action}>
            <Text style={{ color: theme.colors.text }}>Decline</Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            disabled={respond.isPending}
            onPress={() => respond.mutate({ assignmentId: assignment.id, action: 'unable' })}
            style={styles.action}>
            <Text style={{ color: theme.colors.text }}>Unable</Text>
          </Pressable>
        </View>
      ))}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  action: {
    minHeight: 44,
    justifyContent: 'center',
  },
});
