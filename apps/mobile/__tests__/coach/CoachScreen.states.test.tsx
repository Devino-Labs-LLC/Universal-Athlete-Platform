import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { CoachScreen } from '@/src/features/coach/screens/CoachScreen';

let mockCoachHarness: {
  summary: {
    isLoading: boolean;
    isError: boolean;
    data?: undefined;
    error: unknown;
    refetch: () => void;
  };
  conversation: { isLoading: boolean; data?: undefined };
  send: { isPending: boolean; isError: boolean; error: unknown; mutate: () => void };
  clear: { mutate: () => void };
  draft: string;
  setDraft: () => void;
};

jest.mock('@/src/features/coach/hooks/useCoach', () => {
  const actual = jest.requireActual('@/src/features/coach/hooks/useCoach');
  return {
    ...actual,
    useCoach: () => mockCoachHarness,
  };
});

function idleCoach() {
  return {
    summary: { isLoading: false, isError: false, data: undefined, error: null, refetch: jest.fn() },
    conversation: { isLoading: false, data: undefined },
    send: { isPending: false, isError: false, error: null, mutate: jest.fn() },
    clear: { mutate: jest.fn() },
    draft: '',
    setDraft: jest.fn(),
  };
}

describe('CoachScreen states', () => {
  it('shows a loading state before facts are available', async () => {
    mockCoachHarness = { ...idleCoach(), summary: { ...idleCoach().summary, isLoading: true } };
    const { getByLabelText } = await render(
      <ThemeProvider>
        <CoachScreen />
      </ThemeProvider>,
    );
    expect(getByLabelText('Loading Coach…')).toBeTruthy();
  });

  it('shows a summary failure without implying readiness changed', async () => {
    mockCoachHarness = { ...idleCoach(), summary: { ...idleCoach().summary, isError: true } };
    const { getByText } = await render(
      <ThemeProvider>
        <CoachScreen />
      </ThemeProvider>,
    );
    expect(getByText('Coach could not answer just now.')).toBeTruthy();
  });
});
