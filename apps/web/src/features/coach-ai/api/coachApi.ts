import type { ApiClient } from '@/core/api/apiClient';

export interface CoachFact {
  topic: string;
  availability: 'PRESENT' | 'UNAVAILABLE' | string;
  stale: boolean;
  text: string;
}

export interface CoachSummary {
  aiEnabled: boolean;
  asOf: string;
  facts: CoachFact[];
}

export interface CoachMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  contentKind: 'EXPLANATION' | 'FORECAST' | 'REFUSAL' | 'FALLBACK' | string;
  text: string;
  forecast: boolean;
  uncertainty: string | null;
  createdAt: string;
}

export interface CoachConversation {
  id: string | null;
  updatedAt: string | null;
  messages: CoachMessage[];
}

export interface CoachReply {
  conversationId: string;
  messageId: string;
  contentKind: string;
  text: string;
  forecast: boolean;
  uncertainty: string | null;
  contributingSignals: string[];
  deterministicFacts: string[];
}

export const COACH_STARTERS = [
  'How am I looking today?',
  'Why is my readiness lower or higher today?',
  'Why did UAP recommend this?',
  'What should I focus on today?',
  'How has my recent training affected today?',
  'What should I keep in mind for the next few days?',
] as const;

export async function fetchCoachSummary(client: ApiClient): Promise<CoachSummary> {
  const response = await client.axios.get<CoachSummary>('/api/v1/coach/summary');
  return response.data;
}

export async function fetchCurrentConversation(client: ApiClient): Promise<CoachConversation> {
  const response = await client.axios.get<CoachConversation>('/api/v1/coach/conversations/current');
  return response.data;
}

export async function startCoachConversation(client: ApiClient): Promise<string> {
  const response = await client.axios.post<{ id: string }>('/api/v1/coach/conversations');
  return response.data.id;
}

export async function sendCoachMessage(
  client: ApiClient,
  conversationId: string,
  text: string,
): Promise<CoachReply> {
  const response = await client.axios.post<CoachReply>(
    `/api/v1/coach/conversations/${conversationId}/messages`,
    { text },
  );
  return response.data;
}

export async function clearCoachConversations(client: ApiClient): Promise<void> {
  await client.axios.delete('/api/v1/coach/conversations');
}
