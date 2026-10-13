import { useState, type FormEvent } from 'react';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { listSponsors } from '@/features/sponsorship/api/sponsorshipApi';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useQuery } from '@tanstack/react-query';
import { useMarketplaceAdmin } from '@/features/marketplace/hooks/useMarketplace';
import styles from '@/features/marketplace/pages/MarketplacePage.module.scss';

export function PlatformMarketplacePage() {
  const { apiClient } = useAuthSession();
  const admin = useMarketplaceAdmin();
  const sponsors = useQuery({
    queryKey: ['sponsorship', 'sponsors'],
    queryFn: () => listSponsors(apiClient),
  });
  const [partnerId, setPartnerId] = useState('');
  const [title, setTitle] = useState('');
  const [shortDescription, setShortDescription] = useState('');
  const [fullDescription, setFullDescription] = useState('');
  const [destinationUrl, setDestinationUrl] = useState('');
  const [categoryCode, setCategoryCode] = useState('EQUIPMENT');
  const [listingType, setListingType] = useState('PRODUCT');
  const [notice, setNotice] = useState<string | null>(null);

  if (admin.listings.isLoading) {
    return <LoadingView message="Loading marketplace catalog…" />;
  }
  if (admin.listings.isError) {
    return <ErrorView message="Marketplace operator catalog is not available." />;
  }

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    try {
      await admin.createListing({
        partnerId,
        title,
        shortDescription,
        fullDescription,
        listingType,
        categoryCode,
        status: 'ACTIVE',
        featured: false,
        audience: 'ALL',
        destinationUrl,
        startsAt: new Date().toISOString(),
      });
      setTitle('');
      setShortDescription('');
      setFullDescription('');
    } catch {
      setNotice('Could not create the listing. Check HTTPS URLs, plain text, and an ACTIVE partner.');
    }
  };

  return (
    <Page
      title="Marketplace catalog"
      description="UAP-controlled listings. Sponsorship campaigns are separate and are not converted automatically."
    >
      {notice ? <p className={styles.notice}>{notice}</p> : null}
      <section className={styles.section}>
        <h2>Create listing</h2>
        <form className={styles.form} onSubmit={(event) => void onCreate(event)}>
          <select value={partnerId} onChange={(event) => setPartnerId(event.target.value)} required>
            <option value="">Partner</option>
            {(sponsors.data ?? []).map((sponsor) => (
              <option key={sponsor.id} value={sponsor.id}>
                {sponsor.displayName}
              </option>
            ))}
          </select>
          <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Title" required />
          <input
            value={shortDescription}
            onChange={(event) => setShortDescription(event.target.value)}
            placeholder="Short description"
            required
          />
          <textarea
            value={fullDescription}
            onChange={(event) => setFullDescription(event.target.value)}
            placeholder="Full description"
            required
          />
          <input
            value={destinationUrl}
            onChange={(event) => setDestinationUrl(event.target.value)}
            placeholder="https://partner.example/offer"
            required
          />
          <select value={categoryCode} onChange={(event) => setCategoryCode(event.target.value)}>
            <option value="TRAINING">Training</option>
            <option value="RECOVERY">Recovery</option>
            <option value="EQUIPMENT">Equipment</option>
            <option value="APPAREL">Apparel</option>
            <option value="NUTRITION">Nutrition</option>
            <option value="SERVICES">Services</option>
            <option value="CAMPS_PROGRAMS">Camps & Programs</option>
          </select>
          <select value={listingType} onChange={(event) => setListingType(event.target.value)}>
            <option value="PRODUCT">Product</option>
            <option value="SERVICE">Service</option>
            <option value="PROGRAM">Program</option>
            <option value="EVENT">Event</option>
            <option value="OFFER">Offer</option>
          </select>
          <Button type="submit">Create listing</Button>
        </form>
      </section>
      <section className={styles.section}>
        <h2>Listings</h2>
        <ul>
          {(admin.listings.data ?? []).map((listing) => (
            <li key={listing.id}>
              {listing.title} · {listing.status}{' '}
              <Button type="button" variant="ghost" onClick={() => void admin.transition(listing.id, 'PAUSED')}>
                Pause
              </Button>
              <Button type="button" variant="ghost" onClick={() => void admin.transition(listing.id, 'ACTIVE')}>
                Activate
              </Button>
              <Button type="button" variant="ghost" onClick={() => void admin.transition(listing.id, 'ARCHIVED')}>
                Archive
              </Button>
            </li>
          ))}
        </ul>
      </section>
      <section className={styles.section}>
        <h2>Reporting</h2>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Listing</th>
              <th>Partner</th>
              <th>Impressions</th>
              <th>Clicks</th>
              <th>Saves</th>
            </tr>
          </thead>
          <tbody>
            {(admin.reports.data ?? []).map((row) => (
              <tr key={row.listingId}>
                <td>{row.title}</td>
                <td>{row.partnerName}</td>
                <td>{row.impressions}</td>
                <td>{row.outboundClicks}</td>
                <td>{row.saves}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </Page>
  );
}
