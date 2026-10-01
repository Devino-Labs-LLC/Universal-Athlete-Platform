import type { ApiClient } from '@/core/api/apiClient';
import { bindApiClientConnections } from '@uap/connected-apps-contracts';

/** Web ApiClient binding — thin re-export of shared contracts binder. */
export const {
  listConnections,
  beginConnect,
  confirmConnection,
  disconnectConnection,
  requestConnectionSync,
} = bindApiClientConnections<ApiClient>();
