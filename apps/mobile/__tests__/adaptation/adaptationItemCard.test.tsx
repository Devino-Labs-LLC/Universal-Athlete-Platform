import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { AdaptationItemCard } from '@/src/features/adaptation/components/AdaptationItemCard';

import { manualProposalFixture } from './fixtures/proposalFixtures';

describe('AdaptationItemCard', () => {
  const item = manualProposalFixture.items[0];

  afterEach(() => {
    cleanup();
  });

  it('renders accept and reject CTAs for pending substitute items', async () => {
    const onAccept = jest.fn();
    const onReject = jest.fn();
    const { getByTestId } = await render(
      <ThemeProvider>
        <AdaptationItemCard
          item={item}
          proposalStatus="DRAFT"
          onAccept={onAccept}
          onReject={onReject}
          onChooseAnother={jest.fn()}
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId(`accept-item-${item.id}`));
    fireEvent.press(getByTestId(`reject-item-${item.id}`));
    expect(onAccept).toHaveBeenCalled();
    expect(onReject).toHaveBeenCalled();
  });

  it('shows unresolved messaging without accept CTA', async () => {
    const unresolvedItem = {
      ...item,
      action: 'UNRESOLVED' as const,
      athleteDecision: 'PENDING' as const,
    };
    const { queryByTestId, findByText } = await render(
      <ThemeProvider>
        <AdaptationItemCard item={unresolvedItem} proposalStatus="DRAFT" />
      </ThemeProvider>,
    );
    expect(await findByText(/No compatible alternative found/)).toBeTruthy();
    expect(queryByTestId(`accept-item-${item.id}`)).toBeNull();
  });
});
