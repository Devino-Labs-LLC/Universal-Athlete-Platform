import { Link } from 'react-router-dom';

import { useBootstrap } from '@/app/providers/BootstrapProvider';
import { Page } from '@/core/components/Page';
import { Button } from '@/core/components/Button';

export function IncompatiblePage() {
  const { bootstrap } = useBootstrap();

  return (
    <Page
      title="Update required"
      description="This web client is not compatible with the current training contract."
      padded
    >
      <div className="card">
        <p>
          Expected contract version <strong>V1</strong>
          {bootstrap?.clientContractVersion
            ? `, but received ${bootstrap.clientContractVersion}.`
            : '.'}
        </p>
        <p>Install an updated web build or contact support.</p>
        <Link to="/auth/login">
          <Button type="button" variant="secondary">
            Back to login
          </Button>
        </Link>
      </div>
    </Page>
  );
}
