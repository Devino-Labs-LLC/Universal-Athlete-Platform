import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { isApiError } from '@/core/api/errors';
import {
  clearCoachConversations,
  fetchCoachSummary,
  fetchCurrentConversation,
  sendCoachMessage,
  startCoachConversation,
} from '@/features/coach-ai/api/coachApi';

export const coachKeys = {
  summary: ['coach', 'summary'] as const,
  current: ['coach', 'current'] as const,
};

export function coachFailureMessage(error: unknown): string {
  if (isApiError(error) && (error.code === 'AI_UNAVAILABLE' || error.status === 503)) {
    return 'AI Coach is unavailable right now. Your UAP readiness and recommendations are unchanged.';
  }
  if (isApiError(error) && (error.category === 'NETWORK' || error.category === 'TIMEOUT')) {
    return 'You appear to be offline. Deterministic UAP state is unchanged.';
  }
  return 'Coach could not answer just now.';
}

export function useCoach() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState('');

  const summary = useQuery({
    queryKey: coachKeys.summary,
    queryFn: () => fetchCoachSummary(apiClient),
  });
  const conversation = useQuery({
    queryKey: coachKeys.current,
    queryFn: () => fetchCurrentConversation(apiClient),
  });

  const send = useMutation({
    mutationFn: async (text: string) => {
      const conversationId = conversation.data?.id ?? (await startCoachConversation(apiClient));
      return sendCoachMessage(apiClient, conversationId, text);
    },
    onSuccess: async () => {
      setDraft('');
      await queryClient.invalidateQueries({ queryKey: coachKeys.current });
    },
  });

  const clear = useMutation({
    mutationFn: () => clearCoachConversations(apiClient),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: coachKeys.current });
    },
  });

  return { summary, conversation, send, clear, draft, setDraft };
}
