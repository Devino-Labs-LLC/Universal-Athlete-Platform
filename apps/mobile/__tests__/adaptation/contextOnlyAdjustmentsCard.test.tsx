import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ContextOnlyAdjustmentsCard } from '@/src/features/adaptation/components/ContextOnlyAdjustmentsCard';

describe('ContextOnlyAdjustmentsCard', () => {
  it('explains that apply will not change sets/reps/load/duration', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <ContextOnlyAdjustmentsCard
          adjustments={[
            {
              type: 'REDUCE_VOLUME',
              applicability: 'CONTEXT_ONLY',
              orderIndex: 1,
              explanationKey: 'REDUCE_SESSION_VOLUME',
            },
          ]}
        />
      </ThemeProvider>,
    );

    expect(getByText(/will not change sets, reps, load, or duration/)).toBeTruthy();
    expect(getByText(/Reduce Volume/i)).toBeTruthy();
  });

  it('renders nothing when adjustments list is empty', async () => {
    const { queryByTestId } = await render(
      <ThemeProvider>
        <ContextOnlyAdjustmentsCard adjustments={[]} />
      </ThemeProvider>,
    );
    expect(queryByTestId('context-only-adjustments-card')).toBeNull();
  });
});
