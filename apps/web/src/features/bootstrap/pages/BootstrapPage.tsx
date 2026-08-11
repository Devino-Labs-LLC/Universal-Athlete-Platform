import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useBootstrap } from '@/app/providers/BootstrapProvider';
import { LoadingView } from '@/core/components/LoadingView';
import { ErrorView } from '@/core/components/ErrorView';
import { Page } from '@/core/components/Page';

export function BootstrapPage() {
  const { status: authStatus } = useAuthSession();
  const { status, errorMessage, retry } = useBootstrap();

  if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  if (status === 'BOOTSTRAP_ERROR') {
    return (
      <Page title="Unable to start">
        <ErrorView message={errorMessage ?? 'Bootstrap failed'} onRetry={() => void retry()} />
      </Page>
    );
  }

  return <LoadingView message="Preparing your training workspace…" />;
}
