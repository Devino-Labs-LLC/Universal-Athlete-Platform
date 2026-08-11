import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { PlanCard } from '@/features/training/components/PlanCard';
import { usePlans } from '@/features/training/hooks/usePlans';

export function PlanListPage() {
  const plansQuery = usePlans();

  if (plansQuery.isLoading) {
    return <LoadingView message="Loading plans…" />;
  }

  if (plansQuery.isError) {
    return <ErrorView message="Unable to load plans." onRetry={() => plansQuery.refetch()} />;
  }

  const plans = plansQuery.data ?? [];

  return (
    <Page
      title="Training plans"
      description="Browse and manage your training plans."
      actions={
        <Link to="/app/training/plans/new">
          <Button type="button">New plan</Button>
        </Link>
      }
    >
      {plans.length === 0 ? (
        <EmptyView title="No plans yet" message="Create your first training plan." />
      ) : (
        <div style={{ display: 'grid', gap: '0.75rem' }}>
          {plans.map((plan) => (
            <PlanCard key={plan.id} plan={plan} />
          ))}
        </div>
      )}
    </Page>
  );
}
