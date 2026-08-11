import { Page } from '@/core/components/Page';
import { Button } from '@/core/components/Button';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';

export function ProfilePlaceholderPage() {
  const { account, logout, logoutAll } = useAuthSession();

  return (
    <Page title="Profile" description="Athlete profile management arrives in a future milestone.">
      <div className="card">
        <h3 className="cardTitle">Account</h3>
        <p>Email: {account?.email ?? 'Unknown'}</p>
        <p>Status: {account?.status ?? 'Unknown'}</p>
        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
          <Button type="button" variant="secondary" onClick={() => void logout()}>
            Logout
          </Button>
          <Button type="button" variant="ghost" onClick={() => void logoutAll()}>
            Logout all devices
          </Button>
        </div>
      </div>
    </Page>
  );
}
