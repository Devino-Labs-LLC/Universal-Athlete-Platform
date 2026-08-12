import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Badge } from '@/core/components/Badge';
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
import manageStyles from '@/features/profile/pages/ManageResource.module.scss';
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
      <div className={manageStyles.hub}>
        <section className={manageStyles.panel} aria-labelledby="sports-list-heading">
          <h2 className={manageStyles.panelTitle} id="sports-list-heading">
            Your sports
          </h2>
          {snapshot.sports.length === 0 ? (
            <p className={manageStyles.empty}>No sports yet.</p>
          ) : (
            <ul className={manageStyles.list}>
              {snapshot.sports.map((sport) => (
                <li key={sport.id} className={manageStyles.row}>
                  <span className={manageStyles.rowLabel}>
                    {sport.sportType === 'OTHER'
                      ? sport.customSportName
                      : formatEnumLabel(sport.sportType)}
                    {sport.primarySport ? (
                      <>
                        {' '}
                        <Badge tone="accent">Primary</Badge>
                      </>
                    ) : null}
                  </span>
                  <span className={manageStyles.rowActions}>
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

        <section className={manageStyles.panel} aria-labelledby="add-sport-heading">
          <h2 className={manageStyles.panelTitle} id="add-sport-heading">
            Add sport
          </h2>
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
      </div>
    </Page>
  );
}
