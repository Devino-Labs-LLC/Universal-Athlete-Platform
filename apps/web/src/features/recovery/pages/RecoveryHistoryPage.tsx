import { useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly } from '@/core/date/dateOnly';
import { CheckInHistoryTable } from '@/features/recovery/components/CheckInHistoryTable';
import { RecoverySubNav } from '@/features/recovery/components/RecoverySubNav';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import { useRecoveryHistory } from '@/features/recovery/hooks/useRecoveryCheckIns';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import {
  dateRangeForHistory,
  isRecoveryHistoryRangeDays,
  RECOVERY_HISTORY_RANGE_OPTIONS,
  type RecoveryHistoryRangeDays,
} from '@/features/recovery/utils/dateRanges';

function parseRangeParam(value: string | null): RecoveryHistoryRangeDays {
  const parsed = Number(value);
  return isRecoveryHistoryRangeDays(parsed) ? parsed : 30;
}

export function RecoveryHistoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const rangeDays = parseRangeParam(searchParams.get('range'));
  const { startDate, endDate } = dateRangeForHistory(rangeDays, todayDateOnly());
  const historyQuery = useRecoveryHistory(startDate, endDate, true);

  function handleRangeChange(next: RecoveryHistoryRangeDays) {
    const params = new URLSearchParams(searchParams);
    params.set('range', String(next));
    setSearchParams(params);
  }

  return (
    <Page
      title="Recovery history"
      description={`Check-ins from ${startDate} to ${endDate}.`}
      width="wide"
      actions={
        <div className={surfaces.rangeGroup} role="group" aria-label="History range">
          {RECOVERY_HISTORY_RANGE_OPTIONS.map((option) => (
            <button
              key={option}
              type="button"
              className={[
                surfaces.rangeButton,
                rangeDays === option ? surfaces.rangeButtonActive : '',
              ]
                .filter(Boolean)
                .join(' ')}
              aria-pressed={rangeDays === option}
              onClick={() => handleRangeChange(option)}
            >
              {option}d
            </button>
          ))}
        </div>
      }
    >
      <RecoverySubNav />

      {historyQuery.isLoading ? <LoadingView message="Loading recovery history…" /> : null}
      {historyQuery.isError ? (
        <ErrorView message={recoveryErrorMessage(historyQuery.error)} onRetry={() => historyQuery.refetch()} />
      ) : null}

      {historyQuery.data ? (
        <section className={surfaces.panel} aria-labelledby="checkins-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="checkins-heading">
              Check-ins
            </h2>
            <span className={surfaces.panelHint}>{rangeDays}-day range</span>
          </div>
          <CheckInHistoryTable days={historyQuery.data.days} />
        </section>
      ) : null}
    </Page>
  );
}
