import { describe, expect, it, vi } from 'vitest';

import { PersonalRecordsPage } from '@/features/performance/pages/PersonalRecordsPage';
import { renderWithProviders, screen } from '@/test/utils';

const usePersonalRecords = vi.fn();

vi.mock('@/features/performance/hooks/usePersonalRecords', () => ({
  usePersonalRecords: (...args: unknown[]) => usePersonalRecords(...args),
}));

describe('PersonalRecordsPage', () => {
  it('requests all record types when no ?type= filter is present', () => {
    usePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PersonalRecordsPage />, { initialEntries: ['/app/performance/records'] });
    expect(usePersonalRecords).toHaveBeenCalledWith({ recordType: undefined });
    expect(screen.getByRole('combobox', { name: 'Type' })).toHaveValue('');
  });

  it('reads the record type filter from the URL', () => {
    usePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PersonalRecordsPage />, { initialEntries: ['/app/performance/records?type=HIGHEST_ESTIMATED_ONE_REP_MAX'] });
    expect(usePersonalRecords).toHaveBeenCalledWith({ recordType: 'HIGHEST_ESTIMATED_ONE_REP_MAX' });
    expect(screen.getByRole('combobox', { name: 'Type' })).toHaveValue('HIGHEST_ESTIMATED_ONE_REP_MAX');
  });

  it('ignores an invalid record type in the URL', () => {
    usePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PersonalRecordsPage />, { initialEntries: ['/app/performance/records?type=NOT_REAL'] });
    expect(usePersonalRecords).toHaveBeenCalledWith({ recordType: undefined });
  });

  it('shows a factual empty state with no records', () => {
    usePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PersonalRecordsPage />, { initialEntries: ['/app/performance/records'] });
    expect(screen.getByText('No personal records')).toBeInTheDocument();
  });
});
