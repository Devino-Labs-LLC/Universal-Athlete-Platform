import type { BadgeTone } from '@/core/components/Badge';
import type { ConnectionLifecycleState } from '@/features/connectedApps/models/connection';
import {
  CONNECTED_APP_PROVIDERS,
  classifySyncFreshness,
  formatInstant,
  lifecycleLabel,
  lifecycleToneKind,
  newRequestId,
  providerDisplayName,
  providerPlatformNote,
  syncFreshnessLabel,
} from '@uap/connected-apps-contracts';

export {
  CONNECTED_APP_PROVIDERS,
  classifySyncFreshness,
  formatInstant,
  lifecycleLabel,
  newRequestId,
  providerDisplayName,
  providerPlatformNote,
  syncFreshnessLabel,
};

export function lifecycleTone(state: ConnectionLifecycleState): BadgeTone {
  return lifecycleToneKind(state);
}
