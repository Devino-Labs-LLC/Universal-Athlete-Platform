import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import { trainingAssignmentListSchema } from '@/features/coach/models/schemas';

export function AssignedWorkCard() {
  const { apiClient, account, status } = useAuthSession();
  const queryClient = useQueryClient();
  const accountId = status === 'AUTHENTICATED' ? account?.accountId : null;

  const query = useQuery({
    queryKey: ['athlete-assignments', accountId ?? ''],
    queryFn: async () => {
      const response = await apiClient.axios.get('/api/v1/athletes/me/training/assignments');
      return trainingAssignmentListSchema.parse(response.data);
    },
    enabled: Boolean(accountId),
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
  if (!accountId || query.isLoading || open.length === 0) {
    return null;
  }

  return (
    <HomeCard title="Coach assignment">
      <p className="emptyHint">
        Assigned by your coach or team. This is not Athlete Readiness guidance.
      </p>
      {open.map((assignment) => (
        <div key={assignment.id}>
          <p>
            {assignment.title}
            {assignment.description ? ` — ${assignment.description}` : ''}
          </p>
          <p>Scheduled {assignment.scheduledDate}</p>
          <Button
            type="button"
            variant="secondary"
            disabled={respond.isPending}
            onClick={() => respond.mutate({ assignmentId: assignment.id, action: 'decline' })}
          >
            Decline
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={respond.isPending}
            onClick={() => respond.mutate({ assignmentId: assignment.id, action: 'unable' })}
          >
            Unable
          </Button>
        </div>
      ))}
    </HomeCard>
  );
}
