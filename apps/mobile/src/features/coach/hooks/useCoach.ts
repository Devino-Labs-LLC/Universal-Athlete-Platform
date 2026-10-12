import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { isApiError } from '@/src/core/api/errors';
import {
  clearCoachConversations,
  fetchCoachSummary,
  fetchCurrentConversation,
  sendCoachMessage,
  startCoachConversation,
} from '@/src/features/coach/api/coachApi';

export function coachFailureMessage(error: unknown): string {
  if (isApiError(error) && (error.code === 'AI_UNAVAILABLE' || error.status === 503)) {
    return 'AI Coach is unavailable right now. Your UAP readiness and recommendations are unchanged.';
  }
  if (isApiError(error) && (error.category === 'network' || error.category === 'timeout')) {
    return 'You appear to be offline. Deterministic UAP state is unchanged.';
  }
  return 'Coach could not answer just now.';
}

export function useCoach() {
  const { apiClient, status } = useAuthSession();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState('');
  const enabled = status === 'AUTHENTICATED';

  const summary = useQuery({
    queryKey: ['coach', 'summary'],
    queryFn: () => fetchCoachSummary(apiClient),
    enabled,
  });
  const conversation = useQuery({
    queryKey: ['coach', 'current'],
    queryFn: () => fetchCurrentConversation(apiClient),
    enabled,
  });

  const send = useMutation({
    mutationFn: async (text: string) => {
      const conversationId = conversation.data?.id ?? (await startCoachConversation(apiClient));
      await sendCoachMessage(apiClient, conversationId, text);
    },
    onSuccess: async () => {
      setDraft('');
      await queryClient.invalidateQueries({ queryKey: ['coach', 'current'] });
    },
  });

  const clear = useMutation({
    mutationFn: () => clearCoachConversations(apiClient),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['coach', 'current'] });
    },
  });

  return { summary, conversation, send, clear, draft, setDraft };
}
