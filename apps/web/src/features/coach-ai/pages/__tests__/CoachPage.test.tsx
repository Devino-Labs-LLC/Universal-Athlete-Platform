import { describe, expect, it, vi } from 'vitest';

import { CoachPanel } from '@/features/coach-ai/pages/CoachPage';
import { renderWithProviders, screen } from '@/test/utils';
import type { CoachConversation, CoachSummary } from '@/features/coach-ai/api/coachApi';

const summary: CoachSummary = {
  aiEnabled: true,
  asOf: '2026-10-08',
  facts: [
    {
      topic: 'Readiness',
      availability: 'PRESENT',
      stale: true,
      text: 'Deterministic UAP readiness score 70. This score is not an AI calculation.',
    },
    {
      topic: 'Recommendation',
      availability: 'UNAVAILABLE',
      stale: false,
      text: 'No current deterministic training recommendation is stored.',
    },
  ],
};

const conversation: CoachConversation = {
  id: 'conv-1',
  updatedAt: '2026-10-08T15:00:00Z',
  messages: [
    {
      id: 'm1',
      role: 'ASSISTANT',
      contentKind: 'FORECAST',
      text: 'AI forecast (not a readiness score): keep tomorrow easy.',
      forecast: true,
      uncertainty: 'Limited data',
      createdAt: '2026-10-08T15:00:01Z',
    },
  ],
};

describe('CoachPanel', () => {
  it('separates stale UAP facts from an AI forecast', () => {
    renderWithProviders(
      <CoachPanel
        summary={summary}
        conversation={conversation}
        draft=""
        sending={false}
        notice={null}
        onDraft={vi.fn()}
        onSend={vi.fn()}
        onClear={vi.fn()}
      />,
    );

    expect(screen.getByText(/does not calculate your readiness score/)).toBeInTheDocument();
    expect(screen.getByText(/Some UAP data is stale/)).toBeInTheDocument();
    expect(screen.getByText(/Recorded by UAP/)).toBeInTheDocument();
    expect(screen.getByText(/AI forecast — not a readiness score/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'How am I looking today?' })).toBeInTheDocument();
  });

  it('shows provider unavailable and no-data states', () => {
    renderWithProviders(
      <CoachPanel
        summary={{
          aiEnabled: false,
          asOf: '2026-10-08',
          facts: [
            {
              topic: 'Profile',
              availability: 'PRESENT',
              stale: false,
              text: 'First name Ada. Body measurements are not included in Coach context.',
            },
            {
              topic: 'Readiness',
              availability: 'UNAVAILABLE',
              stale: false,
              text: 'No current deterministic readiness assessment is stored.',
            },
          ],
        }}
        conversation={{ id: null, updatedAt: null, messages: [] }}
        draft=""
        sending={true}
        notice="AI Coach is unavailable right now. Your UAP readiness and recommendations are unchanged."
        onDraft={vi.fn()}
        onSend={vi.fn()}
        onClear={vi.fn()}
      />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('AI Coach is unavailable');
    expect(screen.getByText(/no readiness, recovery, recommendation/)).toBeInTheDocument();
    expect(screen.getByText(/AI Coach is turned off/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thinking…' })).toBeDisabled();
  });
});
