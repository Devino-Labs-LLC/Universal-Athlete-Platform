import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { CoachPanel } from '@/src/features/coach/screens/CoachScreen';

const summary = {
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
      stale: true,
      text: 'No current deterministic readiness assessment is stored.',
    },
  ],
};

describe('CoachPanel', () => {
  it('shows UAP facts, stale data, no-data, and a disabled provider', async () => {
    const { getByText, getByTestId } = await render(
      <ThemeProvider>
        <CoachPanel
          summary={summary}
          conversation={{
            id: 'conv-1',
            updatedAt: null,
            messages: [
              {
                id: 'm1',
                role: 'ASSISTANT',
                contentKind: 'FORECAST',
                text: 'AI forecast (not a readiness score): keep tomorrow easy.',
                forecast: true,
                uncertainty: 'Limited data',
                createdAt: '2026-10-08T15:00:00Z',
              },
            ],
          }}
          draft=""
          sending={false}
          notice="AI Coach is unavailable right now. Your UAP readiness and recommendations are unchanged."
          onDraft={jest.fn()}
          onSend={jest.fn()}
          onClear={jest.fn()}
        />
      </ThemeProvider>,
    );

    expect(getByText(/does not calculate your readiness score/)).toBeTruthy();
    expect(getByText(/Some UAP data is stale/)).toBeTruthy();
    expect(getByTestId('coach-no-data')).toBeTruthy();
    expect(getByTestId('coach-forecast')).toBeTruthy();
    expect(getByTestId('coach-disabled')).toBeTruthy();
    expect(getByTestId('coach-notice')).toBeTruthy();
    expect(getByText('How am I looking today?')).toBeTruthy();
  });
});
