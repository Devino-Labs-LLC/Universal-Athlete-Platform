/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-expo',
  setupFiles: ['<rootDir>/jest.setup.js'],
  testMatch: ['**/__tests__/**/*.(test|spec).(ts|tsx)'],
  moduleNameMapper: {
    '^@uap/billing-contracts$': '<rootDir>/../../packages/billing-contracts/src/index.ts',
    '^@uap/billing-contracts/(.*)$': '<rootDir>/../../packages/billing-contracts/src/$1',
    '^@uap/connected-apps-contracts$':
      '<rootDir>/../../packages/connected-apps-contracts/src/index.ts',
    '^@uap/connected-apps-contracts/(.*)$':
      '<rootDir>/../../packages/connected-apps-contracts/src/$1',
    '^@/src/app/config/(.*)$': '<rootDir>/src/config/$1',
    '^@/src/app/providers/(.*)$': '<rootDir>/src/providers/$1',
    '^@/src/app/theme/(.*)$': '<rootDir>/src/theme/$1',
    '^@/(.*)$': '<rootDir>/$1',
    '^react-native-health$': '<rootDir>/__mocks__/react-native-health.js',
    '^react-native-health-connect$': '<rootDir>/__mocks__/react-native-health-connect.js',
  },
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '../../packages/billing-contracts/src/**/*.{ts,tsx}',
    '../../packages/connected-apps-contracts/src/**/*.{ts,tsx}',
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['lcov', 'text-summary'],
};
