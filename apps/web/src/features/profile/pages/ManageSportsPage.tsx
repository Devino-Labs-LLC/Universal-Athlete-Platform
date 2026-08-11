import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Button } from '@/core/components/Button';
import { Page } from '@/core/components/Page';
import { AddSportForm } from '@/features/profile/forms/AddSportForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import {
  useAddAthleteSportMutation,
  useDeleteAthleteSportMutation,
  useSetPrimaryAthleteSportMutation,
} from '@/features/profile/hooks/useAthleteSports';
import { addAthleteSportSchema, type AddAthleteSportRequest } from '@/features/profile/schemas';

export function ManageSportsPage() {
  const navigate = useNavigate();
  const { snapshot, refresh } = useAthleteOnboarding();
  const addSportMutation = useAddAthleteSportMutation();
  const setPrimaryMutation = useSetPrimaryAthleteSportMutation();
  const deleteSportMutation = useDeleteAthleteSportMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm<AddAthleteSportRequest>({
    resolver: zodResolver(addAthleteSportSchema),
    defaultValues: {
      sportType: 'GENERAL_FITNESS',
      primarySport: false,
      participationLevel: 'RECREATIONAL',
      yearsExperience: 0,
      seasonStatus: 'YEAR_ROUND',
    },
  });

  const sportType = form.watch('sportType');

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await addSportMutation.mutateAsync({
        ...values,
        customSportName: values.customSportName?.trim() || undefined,
        preferredPosition: values.preferredPosition?.trim() || undefined,
        primarySport: snapshot.sports.length === 0 ? true : values.primarySport,
      });
      form.reset({
        sportType: 'GENERAL_FITNESS',
        primarySport: false,
        participationLevel: 'RECREATIONAL',
        yearsExperience: 0,
        seasonStatus: 'YEAR_ROUND',
      });
      await refresh();
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to add sport'));
    }
  });

  const handleSetPrimary = async (sportId: string) => {
    try {
      await setPrimaryMutation.mutateAsync(sportId);
      await refresh();
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to set primary sport'));
    }
  };

  const handleDelete = async (sportId: string) => {
    try {
      await deleteSportMutation.mutateAsync(sportId);
      await refresh();
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to remove sport'));
    }
  };

  return (
    <Page title="Manage sports">
      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Your sports</h2>
        {snapshot.sports.length === 0 ? (
          <p style={{ color: 'var(--uap-text-secondary)' }}>No sports yet.</p>
        ) : (
          <ul style={{ margin: 0, padding: 0, listStyle: 'none', display: 'grid', gap: '0.75rem' }}>
            {snapshot.sports.map((sport) => (
              <li
                key={sport.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '1rem',
                }}
              >
                <span>
                  {sport.sportType === 'OTHER'
                    ? sport.customSportName
                    : formatEnumLabel(sport.sportType)}
                  {sport.primarySport ? ' (primary)' : ''}
                </span>
                <span style={{ display: 'inline-flex', gap: '0.5rem' }}>
                  {!sport.primarySport ? (
                    <Button
                      variant="ghost"
                      disabled={setPrimaryMutation.isPending}
                      onClick={() => void handleSetPrimary(sport.id)}
                    >
                      Set primary
                    </Button>
                  ) : null}
                  <Button
                    variant="ghost"
                    disabled={deleteSportMutation.isPending}
                    onClick={() => void handleDelete(sport.id)}
                  >
                    Remove
                  </Button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2 className="cardTitle">Add sport</h2>
        <AddSportForm
          control={form.control}
          sportType={sportType}
          onSubmit={() => void onSubmit()}
          submitting={addSportMutation.isPending}
          submitError={submitError}
          showPrimarySport
        />
      </section>

      <Button variant="secondary" onClick={() => navigate('/app/profile')}>
        Back to profile
      </Button>
    </Page>
  );
}
