import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { CandidatePicker } from '@/src/features/adaptation/components/CandidatePicker';

import { manualProposalFixture } from './fixtures/proposalFixtures';

describe('CandidatePicker', () => {
  const alternatives = manualProposalFixture.items[0]!.alternatives.map((candidate) => ({
    source: 'alternative' as const,
    candidate,
  }));

  it('renders alternative candidates and reports the selected definition id', async () => {
    const onSelect = jest.fn();
    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <CandidatePicker
          candidates={alternatives}
          selectedExerciseDefinitionId="def-target-1"
          onSelect={onSelect}
        />
      </ThemeProvider>,
    );

    expect(getByTestId('candidate-picker')).toBeTruthy();
    expect(getByText('Goblet Squat')).toBeTruthy();
    fireEvent.press(getByText('Leg Press'));
    expect(onSelect).toHaveBeenCalledWith(alternatives[1]);
  });

  it('shows an empty state when no candidates are available', async () => {
    const { getByText, queryByTestId } = await render(
      <ThemeProvider>
        <CandidatePicker candidates={[]} onSelect={jest.fn()} />
      </ThemeProvider>,
    );

    expect(getByText('No alternative exercises are available.')).toBeTruthy();
    expect(queryByTestId('candidate-picker')).toBeNull();
  });
});
