import { useState } from 'react';
import { Alert, Linking, Platform, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  isAccountBillingUnavailable,
  useAccountBillingStatus,
  useRestorePremiumMutation,
} from '@/src/features/billing/hooks/useAccountBilling';
import {
  cadenceLabel,
  formatUsd,
  individualPremiumCatalog,
  isStripeManagedChannel,
  premiumOriginLabel,
  storeManagementLabel,
  storeManagementUrl,
} from '@/src/features/billing/models/accountBilling';
import { accountBillingErrorMessage } from '@/src/features/billing/models/errors';

function formatUtc(instant: string): string {
  const parsed = new Date(instant);
  if (Number.isNaN(parsed.getTime())) {
    return instant;
  }
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'UTC',
  }).format(parsed);
}

export function PremiumBillingScreen() {
  const theme = useAppTheme();
  const statusQuery = useAccountBillingStatus();
  const restoreMutation = useRestorePremiumMutation();
  const [actionError, setActionError] = useState<unknown>(null);

  const subscription = statusQuery.data ?? null;
  const lifecycle = subscription?.lifecycleState;
  const billingUnavailable =
    statusQuery.isError && isAccountBillingUnavailable(statusQuery.error);
  const loadError =
    statusQuery.isError && !billingUnavailable ? statusQuery.error : null;
  const visibleError = actionError ?? loadError;
  const stripeManaged =
    subscription != null && isStripeManagedChannel(subscription.managementChannel);
  const storeUrl =
    subscription != null ? storeManagementUrl(subscription.managementChannel) : null;
  const showStoreManage =
    !stripeManaged &&
    storeUrl != null &&
    lifecycle != null &&
    lifecycle !== 'PENDING' &&
    lifecycle !== 'EXPIRED';
  const canRestore = Platform.OS === 'ios' || Platform.OS === 'android';
  const busy = restoreMutation.isPending || statusQuery.isFetching;

  const openStoreManagement = async () => {
    if (!storeUrl) {
      return;
    }
    setActionError(null);
    try {
      await Linking.openURL(storeUrl);
    } catch (cause) {
      setActionError(cause);
    }
  };

  const handleRestore = () => {
    if (busy || !canRestore) {
      return;
    }
    setActionError(null);
    restoreMutation.mutate(undefined, {
      onSuccess: () => {
        Alert.alert('Premium restored', 'Your Premium subscription status was updated.');
      },
      onError: (cause) => {
        setActionError(cause);
      },
    });
  };

  if (statusQuery.isLoading && subscription == null && !statusQuery.isError) {
    return <LoadingView message="Loading billing…" />;
  }

  return (
    <Screen
      scroll
      title="Premium billing"
      description="Prices are shown before applicable taxes."
      testID="premium-billing-screen"
      includeBottomInset
      refreshing={statusQuery.isFetching}
      onRefresh={() => {
        void statusQuery.refetch();
      }}
    >
      <HomeCard eyebrow="Pricing" title={individualPremiumCatalog.name}>
        <Text style={{ color: theme.colors.textMuted }}>
          Locked list prices (tax-exclusive). No trial. Basic athlete features stay available
          without Premium.
        </Text>
        <CompactInfoRow
          label="Monthly"
          value={`${formatUsd(individualPremiumCatalog.monthlyUsd)} / month`}
        />
        <CompactInfoRow
          label="Annual"
          value={`${formatUsd(individualPremiumCatalog.annualUsd)} / year`}
        />
        <Text style={{ color: theme.colors.textMuted }}>
          {Platform.OS === 'ios'
            ? 'Purchase and renew through the App Store on this device.'
            : Platform.OS === 'android'
              ? 'Purchase and renew through Google Play on this device.'
              : 'Purchase Individual Premium on iOS, Android, or the web.'}
        </Text>
      </HomeCard>

      {billingUnavailable ? (
        <HomeCard eyebrow="Status" title="Unavailable">
          <Text style={{ color: theme.colors.textMuted }}>
            Individual Premium billing is not available right now. You can keep using Athlete
            Readiness without a Premium subscription.
          </Text>
        </HomeCard>
      ) : null}

      {subscription && !billingUnavailable ? (
        <HomeCard eyebrow="Subscription" title="Current Premium">
          <CompactInfoRow label="Plan" value={individualPremiumCatalog.name} />
          <CompactInfoRow label="Cadence" value={cadenceLabel(subscription.cadence)} />
          <CompactInfoRow label="State" value={subscription.lifecycleState} />
          <CompactInfoRow
            label="Premium origin"
            value={premiumOriginLabel(subscription.provider)}
          />
          {subscription.currentPeriodEndsAt ? (
            <CompactInfoRow label="Period ends" value={subscription.currentPeriodEndsAt} />
          ) : null}
          {lifecycle === 'PENDING' ? (
            <Text style={{ color: theme.colors.textMuted }}>Checkout is already in progress.</Text>
          ) : null}
          {lifecycle === 'GRACE_PERIOD' && subscription.graceEndsAt ? (
            <>
              <Text style={{ color: theme.colors.textMuted }}>Payment needs attention.</Text>
              <Text style={{ color: theme.colors.textMuted }}>
                Access continues until {formatUtc(subscription.graceEndsAt)} UTC.
              </Text>
            </>
          ) : null}
          {lifecycle === 'PAST_DUE' ? (
            <Text style={{ color: theme.colors.textMuted }}>
              {stripeManaged
                ? 'Billing needs attention. Manage payment method on the web.'
                : 'Billing needs attention. Manage this subscription in the store where it was purchased.'}
            </Text>
          ) : null}
          {lifecycle === 'CANCEL_AT_PERIOD_END' ? (
            <Text style={{ color: theme.colors.textMuted }}>Renewal is scheduled to end.</Text>
          ) : null}
          {lifecycle === 'ACTIVE' || lifecycle === 'TRIALING' ? (
            <Text style={{ color: theme.colors.textMuted }}>
              Your Premium subscription is active.
            </Text>
          ) : null}
          {stripeManaged ? (
            <Text style={{ color: theme.colors.textMuted }}>
              This Premium subscription was purchased on the web (Stripe). Manage payment method,
              invoices, cancel, and reactivate from Premium billing on the website.
            </Text>
          ) : null}
          {showStoreManage ? (
            <Button
              variant="secondary"
              label={storeManagementLabel(subscription.managementChannel)}
              testID="premium-manage-store"
              disabled={busy}
              onPress={() => {
                void openStoreManagement();
              }}
            />
          ) : null}
        </HomeCard>
      ) : null}

      {!billingUnavailable && (subscription == null || lifecycle === 'EXPIRED') ? (
        <HomeCard eyebrow="Status" title="No active Premium">
          <Text style={{ color: theme.colors.textMuted }}>
            You do not have an active Individual Premium subscription on this account.
          </Text>
        </HomeCard>
      ) : null}

      {!billingUnavailable && canRestore ? (
        <View style={styles.actions}>
          <Button
            variant="secondary"
            label={restoreMutation.isPending ? 'Restoring…' : 'Restore purchases'}
            testID="premium-restore"
            disabled={busy}
            loading={restoreMutation.isPending}
            onPress={handleRestore}
          />
        </View>
      ) : null}

      {visibleError ? (
        <ErrorView
          message={accountBillingErrorMessage(visibleError, 'Unable to load billing.')}
          testID="premium-billing-error"
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  actions: {
    gap: 8,
  },
});
