// AsyncStorage 2.x throws at import when the native module is absent.
// Jest has no native module, so use the package's official in-memory mock.
jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);
