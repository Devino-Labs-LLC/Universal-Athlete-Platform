import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ListingCard } from '@/features/marketplace/components/ListingCard';
import {
  useMarketplaceActions,
  useMarketplaceBrowse,
  useMarketplaceCategories,
} from '@/features/marketplace/hooks/useMarketplace';
import styles from '@/features/marketplace/pages/MarketplacePage.module.scss';

export function MarketplacePage() {
  const [params, setParams] = useSearchParams();
  const category = params.get('category') ?? '';
  const type = params.get('type') ?? '';
  const q = params.get('q') ?? '';
  const [search, setSearch] = useState(q);
  const categories = useMarketplaceCategories();
  const browse = useMarketplaceBrowse({
    category: category || undefined,
    type: type || undefined,
    q: q || undefined,
  });
  const { recordImpression } = useMarketplaceActions();
  const impressed = useRef(new Set<string>());

  useEffect(() => {
    for (const listing of browse.data?.listings ?? []) {
      if (!impressed.current.has(listing.id)) {
        impressed.current.add(listing.id);
        void recordImpression(listing.id);
      }
    }
  }, [recordImpression, browse.data?.listings]);

  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    const next = new URLSearchParams(params);
    if (search) {
      next.set('q', search);
    } else {
      next.delete('q');
    }
    setParams(next);
  };

  const setFilter = (key: string, value: string) => {
    const next = new URLSearchParams(params);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    setParams(next);
  };

  const listings = browse.data?.listings ?? [];
  const categoryOptions = useMemo(() => categories.data ?? [], [categories.data]);

  if (browse.isLoading) {
    return <LoadingView message="Loading marketplace…" />;
  }
  if (browse.isError) {
    return <ErrorView message="Marketplace is not available right now." />;
  }

  return (
    <Page
      title="Marketplace"
      description="Curated partner products, programs, and offers. This is not athlete intelligence and does not change readiness."
    >
      <form className={styles.toolbar} onSubmit={onSearch}>
        <label>
          Search
          <input
            className={styles.input}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search listings"
            aria-label="Search marketplace"
          />
        </label>
        <label>
          Category
          <select
            className={styles.select}
            value={category}
            aria-label="Filter by category"
            onChange={(event) => setFilter('category', event.target.value)}
          >
            <option value="">All categories</option>
            {categoryOptions.map((item) => (
              <option key={item.code} value={item.code}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Type
          <select
            className={styles.select}
            value={type}
            aria-label="Filter by listing type"
            onChange={(event) => setFilter('type', event.target.value)}
          >
            <option value="">All types</option>
            <option value="PRODUCT">Product</option>
            <option value="SERVICE">Service</option>
            <option value="PROGRAM">Program</option>
            <option value="EVENT">Event</option>
            <option value="OFFER">Offer</option>
          </select>
        </label>
        <Button type="submit">Search</Button>
      </form>

      {listings.length === 0 ? (
        <EmptyView title="No listings" message="No marketplace listings match these filters." />
      ) : (
        <div className={styles.grid}>
          {listings.map((listing) => (
            <ListingCard key={listing.id} listing={listing} />
          ))}
        </div>
      )}
    </Page>
  );
}
