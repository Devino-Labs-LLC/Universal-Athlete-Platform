import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { formatDateDisplay, parseDateOnly, todayDateOnly } from '@/core/date/dateOnly';
import { RecoveryCheckInForm } from '@/features/recovery/forms/RecoveryCheckInForm';
import { useCheckInMutations } from '@/features/recovery/hooks/useCheckInMutations';
import { useRecoveryCheckInByDate } from '@/features/recovery/hooks/useRecoveryCheckIns';
import { mapCheckInToFormValues } from '@/features/recovery/api/checkInsApi';
import {
  type CreateCheckInFormValues,
  createCheckInFormSchema,
  defaultCheckInFormValues,
} from '@/features/recovery/models/checkInForm';
import { isNotFoundError, isVersionConflictError, recoveryErrorMessage } from '@/features/recovery/models/errors';
import { RecoverySubNav } from '@/features/recovery/components/RecoverySubNav';
import formStyles from '@/features/recovery/forms/RecoveryCheckInForm.module.scss';
import { subtractDays } from '@/features/recovery/utils/dateRanges';

function resolveCheckInDate(raw: string | null): string {
  if (!raw) {
    return todayDateOnly();
  }
  try {
    return parseDateOnly(raw);
  } catch {
    return todayDateOnly();
  }
}

export function RecoveryCheckInPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const checkInDate = resolveCheckInDate(searchParams.get('date'));
  const parsedDate = parseDateOnly(checkInDate);

  const checkInQuery = useRecoveryCheckInByDate(parsedDate);
  const { saveMutation } = useCheckInMutations();

  const existingCheckIn =
    checkInQuery.isError && isNotFoundError(checkInQuery.error) ? null : (checkInQuery.data ?? null);

  const defaultValues = useMemo(
    () => (existingCheckIn ? mapCheckInToFormValues(existingCheckIn) : defaultCheckInFormValues(checkInDate)),
    [existingCheckIn, checkInDate],
  );

  const form = useForm<CreateCheckInFormValues>({
    resolver: zodResolver(createCheckInFormSchema),
    defaultValues,
  });

  const [submitError, setSubmitError] = useState<string | null>(null);

  const sourceKey = `${checkInDate}:${existingCheckIn?.id ?? 'new'}:${existingCheckIn?.version ?? 0}`;
  const lastAppliedSourceKey = useRef<string | null>(null);

  useEffect(() => {
    if (checkInQuery.isLoading) {
      return;
    }
    if (lastAppliedSourceKey.current === sourceKey) {
      return;
    }
    form.reset(defaultValues);
    lastAppliedSourceKey.current = sourceKey;
  }, [checkInQuery.isLoading, defaultValues, form, sourceKey]);

  const validateDate = (): boolean => {
    const today = todayDateOnly();
    if (parsedDate > today) {
      setSubmitError('Check-in date cannot be in the future.');
      return false;
    }
    const earliest = subtractDays(today, 30);
    if (parsedDate < earliest) {
      setSubmitError('Check-in date is outside the allowed range (30 days).');
      return false;
    }
    return true;
  };

  const onSubmit = (values: CreateCheckInFormValues) => {
    if (!validateDate()) {
      return;
    }
    setSubmitError(null);
    const mode = existingCheckIn ? 'update' : 'create';
    saveMutation.mutate(
      {
        mode,
        checkInId: existingCheckIn?.id,
        expectedVersion: existingCheckIn?.version,
        values: { ...values, checkInDate },
      },
      {
        onSuccess: () => {
          void navigate('/app/recovery');
        },
        onError: (error) => {
          if (isVersionConflictError(error)) {
            setSubmitError(recoveryErrorMessage(error));
            void checkInQuery.refetch();
            return;
          }
          setSubmitError(recoveryErrorMessage(error));
        },
      },
    );
  };

  if (checkInQuery.isLoading) {
    return <LoadingView message="Loading check-in…" />;
  }

  if (checkInQuery.isError && !isNotFoundError(checkInQuery.error)) {
    return (
      <ErrorView message={recoveryErrorMessage(checkInQuery.error)} onRetry={() => void checkInQuery.refetch()} />
    );
  }

  const pending = saveMutation.isPending || form.formState.isSubmitting;

  return (
    <Page
      title={existingCheckIn ? 'Update check-in' : 'Recovery check-in'}
      description={`Check-in for ${formatDateDisplay(parsedDate)}. Required ratings first; sleep, discomfort, and notes are optional and omitted when blank — never stored as zero.`}
      width="wide"
    >
      <RecoverySubNav />
      <form className={formStyles.checkInForm} onSubmit={form.handleSubmit(onSubmit)} noValidate>
        <RecoveryCheckInForm
          control={form.control}
          register={form.register}
          errors={form.formState.errors}
          setValue={form.setValue}
        />
        {submitError ? (
          <p className="formError" role="alert" aria-live="assertive">
            {submitError}
          </p>
        ) : null}
        <div className={formStyles.actions}>
          <Button type="submit" disabled={pending}>
            {pending ? 'Saving…' : existingCheckIn ? 'Save changes' : 'Save check-in'}
          </Button>
        </div>
      </form>
    </Page>
  );
}
