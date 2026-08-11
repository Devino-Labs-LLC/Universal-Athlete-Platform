import { describe, expect, it, vi } from 'vitest';

import { HomeDiagnosticPage } from '@/features/home/pages/HomeDiagnosticPage';
import { renderWithProviders, screen } from '@/test/utils';

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: {
      axios: {},
      baseURL: '',
    },
  }),
}));

vi.mock('@tanstack/react-query', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-query')>(
    '@tanstack/react-query',
  );

  return {
    ...actual,
    useQuery: () => ({
      isLoading: false,
      isError: false,
      data: {
        date: '2026-08-11',
        recovery: { checkInPresent: true },
        readiness: { readinessPresent: true, readinessBand: 'GREEN' },
        recommendation: { recommendationPresent: true, overallAction: 'TRAIN' },
        training: { scheduledOccurrenceCount: 2 },
      },
      refetch: vi.fn(),
    }),
  };
});

describe('HomeDiagnosticPage', () => {
  it('renders diagnostic stats from fixture data', () => {
    renderWithProviders(<HomeDiagnosticPage />);

    expect(screen.getByText('Today diagnostic')).toBeInTheDocument();
    expect(screen.getByText('Recovery present')).toBeInTheDocument();
    expect(screen.getByText('Yes')).toBeInTheDocument();
    expect(screen.getByText('GREEN')).toBeInTheDocument();
    expect(screen.getByText('TRAIN')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});
