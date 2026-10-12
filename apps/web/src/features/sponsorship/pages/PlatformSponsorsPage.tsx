import { useState, type FormEvent } from 'react';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { SponsorCard } from '@/features/sponsorship/components/SponsorCard';
import { useSponsorAdmin } from '@/features/sponsorship/hooks/useSponsorship';
import styles from '@/features/sponsorship/pages/PlatformSponsorsPage.module.scss';

export function PlatformSponsorsPage() {
  const admin = useSponsorAdmin();
  const [sponsorName, setSponsorName] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [websiteUrl, setWebsiteUrl] = useState('');
  const [campaignSponsorId, setCampaignSponsorId] = useState('');
  const [campaignName, setCampaignName] = useState('');
  const [headline, setHeadline] = useState('');
  const [body, setBody] = useState('');
  const [ctaLabel, setCtaLabel] = useState('Learn more');
  const [destinationUrl, setDestinationUrl] = useState('');
  const [surface, setSurface] = useState('HOME');
  const [notice, setNotice] = useState<string | null>(null);

  if (admin.sponsors.isLoading) {
    return <LoadingView message="Loading partner catalog…" />;
  }
  if (admin.sponsors.isError) {
    return <ErrorView message="Partner catalog is not available." />;
  }

  const onCreateSponsor = async (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    try {
      await admin.createSponsor({ name: sponsorName, displayName, websiteUrl });
      setSponsorName('');
      setDisplayName('');
    } catch {
      setNotice('Could not create the sponsor.');
    }
  };

  const onCreateCampaign = async (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    try {
      const campaign = await admin.createCampaign({
        sponsorId: campaignSponsorId,
        name: campaignName,
        status: 'ACTIVE',
        startsAt: new Date().toISOString(),
        headline,
        body,
        ctaLabel,
        destinationUrl,
      });
      if (campaign.id) {
        await admin.attachPlacement(campaign.id, { surface, audience: 'ALL' });
      }
    } catch {
      setNotice('Could not create the campaign. Check HTTPS URLs and plain text.');
    }
  };

  const preview = admin.campaigns.data?.[0];

  return (
    <Page
      title="Partner catalog"
      description="UAP-controlled sponsors and campaigns. This is not athlete intelligence and does not change readiness."
    >
      {notice ? <p className={styles.notice}>{notice}</p> : null}

      <section className={styles.section}>
        <h2>Sponsors</h2>
        <ul className={styles.list}>
          {(admin.sponsors.data ?? []).map((sponsor) => (
            <li key={sponsor.id}>
              <strong>{sponsor.displayName}</strong> · {sponsor.status}
              {sponsor.status === 'DRAFT' ? (
                <Button type="button" variant="secondary" onClick={() => void admin.transitionSponsor(sponsor.id, 'ACTIVE')}>
                  Activate
                </Button>
              ) : null}
              {sponsor.status === 'ACTIVE' ? (
                <Button type="button" variant="ghost" onClick={() => void admin.transitionSponsor(sponsor.id, 'PAUSED')}>
                  Pause
                </Button>
              ) : null}
              {sponsor.status === 'PAUSED' ? (
                <Button type="button" variant="ghost" onClick={() => void admin.transitionSponsor(sponsor.id, 'ARCHIVED')}>
                  Archive
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
        <form className={styles.form} onSubmit={(event) => void onCreateSponsor(event)}>
          <input value={sponsorName} onChange={(event) => setSponsorName(event.target.value)} placeholder="Legal name" required />
          <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="Display name" required />
          <input value={websiteUrl} onChange={(event) => setWebsiteUrl(event.target.value)} placeholder="https://website" />
          <Button type="submit">Create sponsor</Button>
        </form>
      </section>

      <section className={styles.section}>
        <h2>Campaigns</h2>
        <ul className={styles.list}>
          {(admin.campaigns.data ?? []).map((campaign) => (
            <li key={campaign.id}>
              <strong>{campaign.name}</strong> · {campaign.status}
              {campaign.status === 'ACTIVE' ? (
                <Button type="button" variant="ghost" onClick={() => void admin.transitionCampaign(campaign.id, 'PAUSED')}>
                  Pause
                </Button>
              ) : null}
              {campaign.status === 'PAUSED' ? (
                <Button type="button" variant="ghost" onClick={() => void admin.transitionCampaign(campaign.id, 'ENDED')}>
                  End
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
        <form className={styles.form} onSubmit={(event) => void onCreateCampaign(event)}>
          <select value={campaignSponsorId} onChange={(event) => setCampaignSponsorId(event.target.value)} required>
            <option value="">Sponsor</option>
            {(admin.sponsors.data ?? []).map((sponsor) => (
              <option key={sponsor.id} value={sponsor.id}>
                {sponsor.displayName}
              </option>
            ))}
          </select>
          <input value={campaignName} onChange={(event) => setCampaignName(event.target.value)} placeholder="Campaign name" required />
          <input value={headline} onChange={(event) => setHeadline(event.target.value)} placeholder="Headline" required />
          <textarea value={body} onChange={(event) => setBody(event.target.value)} placeholder="Body" required />
          <input value={ctaLabel} onChange={(event) => setCtaLabel(event.target.value)} placeholder="CTA" required />
          <input value={destinationUrl} onChange={(event) => setDestinationUrl(event.target.value)} placeholder="https://destination" required />
          <select value={surface} onChange={(event) => setSurface(event.target.value)}>
            <option value="HOME">HOME</option>
            <option value="TRAINING">TRAINING</option>
            <option value="TEAM">TEAM</option>
          </select>
          <Button type="submit">Create campaign</Button>
        </form>
      </section>

      {preview ? (
        <section className={styles.section}>
          <h2>Preview</h2>
          <SponsorCard
            placement={{
              placementId: preview.placements[0]?.id ?? preview.id,
              campaignId: preview.id,
              sponsorId: preview.sponsorId,
              sponsorDisplayName: admin.sponsors.data?.find((item) => item.id === preview.sponsorId)?.displayName ?? 'Partner',
              label: 'Sponsored',
              headline: preview.headline,
              body: preview.body,
              ctaLabel: preview.ctaLabel,
              destinationUrl: preview.destinationUrl,
              creativeUrl: preview.creativeUrl,
              surface: 'HOME',
            }}
          />
        </section>
      ) : null}

      <section className={styles.section}>
        <h2>Reporting</h2>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Sponsor</th>
              <th>Campaign</th>
              <th>Surface</th>
              <th>Impressions</th>
              <th>Clicks</th>
              <th>CTR</th>
            </tr>
          </thead>
          <tbody>
            {(admin.reports.data ?? []).map((row) => (
              <tr key={`${row.campaignId}-${row.surface}`}>
                <td>{row.sponsorName}</td>
                <td>{row.campaignName}</td>
                <td>{row.surface}</td>
                <td>{row.impressions}</td>
                <td>{row.clicks}</td>
                <td>{row.clickThroughRate.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </Page>
  );
}
