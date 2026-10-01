/**
 * Jest stub — CI has no Health Connect native module.
 * Production uses react-native-health-connect via the androidHealthConnect adapter (lazy require).
 */
module.exports = {
  getSdkStatus: async () => 1,
  initialize: async () => false,
  requestPermission: async () => [],
  getGrantedPermissions: async () => [],
  readRecords: async () => ({ records: [] }),
  openHealthConnectSettings: () => undefined,
  SdkAvailabilityStatus: {
    SDK_UNAVAILABLE: 1,
    SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED: 2,
    SDK_AVAILABLE: 3,
  },
};
